-- 0009: push notifications (Firebase Cloud Messaging via the `send-push` Edge Function).
--
--   * device_tokens: which phone belongs to which user. Written only through the two RPCs below.
--   * Triggers call the Edge Function (with pg_net) when:
--       - staff/admins add a timeline entry  -> the resident who filed the report
--       - a resident files a report          -> every staff member and admin
--       - a post is published                -> everyone (FCM topic "bulletin"; emergencies use a high-priority channel)
--
-- Before this does anything, store two values in Vault (SQL Editor, once — see supabase/README.md):
--   select vault.create_secret('https://<project-ref>.supabase.co/functions/v1/send-push', 'push_function_url');
--   select vault.create_secret('<long random string>', 'push_webhook_secret');
-- Until then the triggers do nothing, and they never block or fail the insert that fired them.

create extension if not exists pg_net with schema extensions;

-- ─── Device tokens ───────────────────────────────────────────────────────────
create table if not exists public.device_tokens (
    token      text primary key,
    user_id    uuid not null references public.profiles (id) on delete cascade,
    platform   text not null default 'android',
    updated_at timestamptz not null default now()
);
create index if not exists device_tokens_user_idx on public.device_tokens (user_id);

-- No direct access for app users; the Edge Function reads it with the service role.
revoke all on public.device_tokens from anon, authenticated;
alter table public.device_tokens enable row level security;

-- Links a token to the caller. A phone that changes accounts moves its token to the new user.
create or replace function public.register_device_token(p_token text, p_platform text default 'android')
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if auth.uid() is null or p_token is null or length(p_token) < 20 or length(p_token) > 4096 then
        return;
    end if;
    insert into public.device_tokens (token, user_id, platform)
    values (p_token, auth.uid(), left(coalesce(p_platform, 'android'), 20))
    on conflict (token) do update
        set user_id = excluded.user_id, platform = excluded.platform, updated_at = now();
end;
$$;

-- Removes the caller's own token (on sign-out).
create or replace function public.unregister_device_token(p_token text)
returns void
language sql security definer set search_path = ''
as $$
    delete from public.device_tokens where token = p_token and user_id = auth.uid();
$$;

revoke all on function public.register_device_token(text, text) from public, anon;
revoke all on function public.unregister_device_token(text) from public, anon;
grant execute on function public.register_device_token(text, text) to authenticated;
grant execute on function public.unregister_device_token(text) to authenticated;

-- ─── Calling the Edge Function ───────────────────────────────────────────────
create or replace function public.notify_push(p_event text, p_record jsonb)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    fn_url text;
    secret text;
begin
    select decrypted_secret into fn_url from vault.decrypted_secrets where name = 'push_function_url';
    select decrypted_secret into secret from vault.decrypted_secrets where name = 'push_webhook_secret';
    if fn_url is null or secret is null then
        return;  -- push not set up yet
    end if;
    perform net.http_post(
        url := fn_url,
        body := jsonb_build_object('event', p_event, 'record', p_record),
        headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', secret),
        timeout_milliseconds := 5000
    );
exception when others then
    -- A notification problem must never stop a report, update or post from being saved.
    raise warning 'notify_push(%) failed: %', p_event, sqlerrm;
end;
$$;

revoke all on function public.notify_push(text, jsonb) from public, anon, authenticated;

create or replace function public.push_on_report_update()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    -- Only LGU actions notify the resident (the first "pending" entry is their own submission).
    if exists (select 1 from public.profiles where id = new.author_id and role in ('staff', 'admin')) then
        perform public.notify_push('report_update', to_jsonb(new));
    end if;
    return new;
end;
$$;

drop trigger if exists report_updates_push on public.report_updates;
create trigger report_updates_push after insert on public.report_updates
    for each row execute function public.push_on_report_update();

create or replace function public.push_on_new_report()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    perform public.notify_push('new_report', jsonb_build_object(
        'id', new.id, 'reporter_id', new.reporter_id, 'title', new.title, 'category', new.category
    ));
    return new;
end;
$$;

drop trigger if exists reports_push on public.reports;
create trigger reports_push after insert on public.reports
    for each row execute function public.push_on_new_report();

create or replace function public.push_on_new_post()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    -- Scheduled posts (published_at in the future) aren't announced here.
    if new.published_at <= now() + interval '1 minute' then
        perform public.notify_push('new_post', jsonb_build_object(
            'id', new.id, 'type', new.type, 'title', new.title, 'body', left(new.body, 300)
        ));
    end if;
    return new;
end;
$$;

drop trigger if exists posts_push on public.posts;
create trigger posts_push after insert on public.posts
    for each row execute function public.push_on_new_post();
