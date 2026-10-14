-- Run after 0006. Comments, event details, barangay list, resolution ratings,
-- self-service account deletion, and an admin activity log.

-- ─── Barangays (admin-managed list used by reports and profiles) ─────────────
create table if not exists public.barangays (
    id          uuid primary key default gen_random_uuid(),
    name        text not null unique check (char_length(name) between 2 and 80),
    created_at  timestamptz not null default now()
);

revoke all on public.barangays from anon, authenticated;
grant select on public.barangays to anon, authenticated;
grant insert (name) on public.barangays to authenticated;
grant update (name) on public.barangays to authenticated;
grant delete on public.barangays to authenticated;
alter table public.barangays enable row level security;
create policy "Anyone reads barangays" on public.barangays for select to anon, authenticated using (true);
create policy "Admins add barangays" on public.barangays for insert to authenticated with check (public.is_admin());
create policy "Admins rename barangays" on public.barangays for update to authenticated
    using (public.is_admin()) with check (public.is_admin());
create policy "Admins delete barangays" on public.barangays for delete to authenticated using (public.is_admin());

alter table public.reports
    add column if not exists barangay_id uuid references public.barangays (id) on delete set null;
grant insert (barangay_id) on public.reports to authenticated;

-- ─── Resolution rating ("Was this fixed?") ───────────────────────────────────
alter table public.reports
    add column if not exists resolution_rating smallint check (resolution_rating in (-1, 1)),
    add column if not exists resolution_feedback text check (char_length(resolution_feedback) <= 500);

-- Reporter rates their own resolved report (columns aren't granted to clients, so this is the only way in).
create or replace function public.rate_resolution(target_report uuid, rating smallint, feedback text default null)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if rating not in (-1, 1) then
        raise exception 'Rating must be -1 or 1';
    end if;
    update public.reports
    set resolution_rating = rating, resolution_feedback = nullif(trim(feedback), '')
    where id = target_report and reporter_id = auth.uid() and status = 'resolved';
    if not found then
        raise exception 'Only your own resolved reports can be rated' using errcode = '42501';
    end if;
end;
$$;
revoke execute on function public.rate_resolution(uuid, smallint, text) from public, anon;
grant execute on function public.rate_resolution(uuid, smallint, text) to authenticated;

-- ─── Event details on posts ─────────────────────────────────────────────────
alter table public.posts
    add column if not exists event_start timestamptz,
    add column if not exists event_end timestamptz,
    add column if not exists event_location text check (char_length(event_location) <= 200);
grant insert (event_start, event_end, event_location) on public.posts to authenticated;
grant update (event_start, event_end, event_location) on public.posts to authenticated;

-- ─── Comments on bulletin posts ──────────────────────────────────────────────
create table if not exists public.post_comments (
    id           uuid primary key default gen_random_uuid(),
    post_id      uuid not null references public.posts (id) on delete cascade,
    user_id      uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    -- Copied from the profile when posting, because citizens can't read other people's profiles.
    author_name  text,
    body         text not null check (char_length(body) between 1 and 1000),
    created_at   timestamptz not null default now()
);
create index if not exists post_comments_post_idx on public.post_comments (post_id, created_at);

revoke all on public.post_comments from anon, authenticated;
grant select on public.post_comments to authenticated;
grant insert (post_id, body) on public.post_comments to authenticated;
grant delete on public.post_comments to authenticated;
alter table public.post_comments enable row level security;
create policy "Signed-in users read comments" on public.post_comments for select to authenticated using (true);
create policy "Users comment as themselves" on public.post_comments for insert to authenticated
    with check (user_id = auth.uid());
-- Moderation: admins can remove any comment; everyone can remove their own.
create policy "Remove own comment or moderate" on public.post_comments for delete to authenticated
    using (user_id = auth.uid() or public.is_admin());

create or replace function public.fill_comment_author()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if (select count(*) from public.post_comments
        where user_id = new.user_id and created_at > now() - interval '1 hour') >= 30 then
        raise exception 'Too many comments in the last hour. Please try again later.' using errcode = 'P0001';
    end if;
    new.author_name := coalesce(
        (select nullif(trim(full_name), '') from public.profiles where id = new.user_id),
        'Resident'
    );
    return new;
end;
$$;
drop trigger if exists post_comments_fill_author on public.post_comments;
create trigger post_comments_fill_author before insert on public.post_comments
    for each row execute function public.fill_comment_author();

alter publication supabase_realtime add table public.post_comments;

-- ─── Delete my account ───────────────────────────────────────────────────────
-- Deletes the caller's auth user; profiles, reports, reactions, and comments cascade.
-- The app removes the user's photos from storage first. Admins must be demoted first so the LGU never loses its last admin by accident.
create or replace function public.delete_my_account()
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if public.is_admin() then
        raise exception 'Admins cannot delete their account. Ask another admin to change your role first.'
            using errcode = '42501';
    end if;
    delete from auth.users where id = auth.uid();
end;
$$;
revoke execute on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;

-- Users may delete their own report photos (needed before deleting the account).
create policy "Users delete own report photos"
    on storage.objects for delete to authenticated
    using (bucket_id = 'report-photos' and (storage.foldername(name))[1] = auth.uid()::text);

-- ─── Activity log (admins only) ──────────────────────────────────────────────
create table if not exists public.audit_log (
    id           bigint generated always as identity primary key,
    actor_id     uuid,
    actor_name   text,
    action       text not null,           -- e.g. 'report.status', 'user.role', 'post.delete'
    target_type  text not null,
    target_id    text,
    summary      text not null,           -- human-readable English line shown in the app
    created_at   timestamptz not null default now()
);
create index if not exists audit_log_created_idx on public.audit_log (created_at desc);

revoke all on public.audit_log from anon, authenticated;
grant select on public.audit_log to authenticated;
alter table public.audit_log enable row level security;
create policy "Admins read the activity log" on public.audit_log for select to authenticated using (public.is_admin());
-- No insert/update/delete grants: rows are written only by the triggers below, and can't be edited from the app.

create or replace function public.write_audit(p_action text, p_target_type text, p_target_id text, p_summary text)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.audit_log (actor_id, actor_name, action, target_type, target_id, summary)
    values (
        auth.uid(),
        (select coalesce(nullif(trim(full_name), ''), email) from public.profiles where id = auth.uid()),
        p_action, p_target_type, p_target_id, p_summary
    );
end;
$$;
revoke execute on function public.write_audit(text, text, text, text) from public, anon, authenticated;

create or replace function public.audit_report_update()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
    report_title text;
begin
    select title into report_title from public.reports where id = new.report_id;
    if new.status is not null and new.status <> 'pending' then
        perform public.write_audit('report.status', 'report', new.report_id::text,
            format('Set "%s" to %s', report_title, new.status));
    elsif new.status is null then
        perform public.write_audit('report.note', 'report', new.report_id::text,
            format('Added a note on "%s"', report_title));
    end if;
    return new;
end;
$$;
drop trigger if exists report_updates_audit on public.report_updates;
create trigger report_updates_audit after insert on public.report_updates
    for each row execute function public.audit_report_update();

create or replace function public.audit_report_assign()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if new.assigned_to is distinct from old.assigned_to then
        perform public.write_audit('report.assign', 'report', new.id::text,
            case when new.assigned_to is null then format('Unassigned "%s"', new.title)
                 else format('Assigned "%s" to %s', new.title,
                      (select coalesce(nullif(trim(full_name), ''), email) from public.profiles where id = new.assigned_to))
            end);
    end if;
    return new;
end;
$$;
drop trigger if exists reports_assign_audit on public.reports;
create trigger reports_assign_audit after update of assigned_to on public.reports
    for each row execute function public.audit_report_assign();

create or replace function public.audit_role_change()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if new.role is distinct from old.role then
        perform public.write_audit('user.role', 'user', new.id::text,
            format('Changed %s from %s to %s', coalesce(nullif(trim(new.full_name), ''), new.email), old.role, new.role));
    end if;
    return new;
end;
$$;
drop trigger if exists profiles_role_audit on public.profiles;
create trigger profiles_role_audit after update of role on public.profiles
    for each row execute function public.audit_role_change();

create or replace function public.audit_post()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if tg_op = 'INSERT' then
        perform public.write_audit('post.create', 'post', new.id::text, format('Posted "%s"', new.title));
    elsif tg_op = 'UPDATE' then
        perform public.write_audit('post.edit', 'post', new.id::text, format('Edited "%s"', new.title));
    else
        perform public.write_audit('post.delete', 'post', old.id::text, format('Deleted "%s"', old.title));
    end if;
    return coalesce(new, old);
end;
$$;
drop trigger if exists posts_audit on public.posts;
create trigger posts_audit after insert or update or delete on public.posts
    for each row execute function public.audit_post();

create or replace function public.audit_hotline()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if tg_op = 'INSERT' then
        perform public.write_audit('hotline.create', 'hotline', new.id::text, format('Added hotline %s (%s)', new.name, new.phone));
    elsif tg_op = 'UPDATE' then
        perform public.write_audit('hotline.edit', 'hotline', new.id::text, format('Edited hotline %s (%s)', new.name, new.phone));
    else
        perform public.write_audit('hotline.delete', 'hotline', old.id::text, format('Deleted hotline %s', old.name));
    end if;
    return coalesce(new, old);
end;
$$;
drop trigger if exists emergency_contacts_audit on public.emergency_contacts;
create trigger emergency_contacts_audit after insert or update or delete on public.emergency_contacts
    for each row execute function public.audit_hotline();

create or replace function public.audit_comment_delete()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    -- Only log moderation (an admin removing someone else's comment).
    if old.user_id is distinct from auth.uid() then
        perform public.write_audit('comment.delete', 'comment', old.id::text,
            format('Removed a comment by %s', coalesce(old.author_name, 'a resident')));
    end if;
    return old;
end;
$$;
drop trigger if exists post_comments_audit on public.post_comments;
create trigger post_comments_audit after delete on public.post_comments
    for each row execute function public.audit_comment_delete();
