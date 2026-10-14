-- 0008: accountability — who posted / edited each bulletin post, who wrote each timeline entry and comment
-- (with their role), and which staff/admins have opened ("seen") each report — visible to staff and admins only.
--
-- Names and roles are copied onto the rows by triggers, because citizens can't read other people's profiles.
-- Clients can't write these columns (posts / report_updates / post_comments use column-level grants), so the
-- values always come from the database.

-- ─── Bulletin posts: posted by / last edited by ──────────────────────────────
alter table public.posts
    add column if not exists author_name    text,
    add column if not exists author_role    public.user_role,
    add column if not exists edited_by_name text,
    add column if not exists edited_at      timestamptz;

-- Existing posts (before the trigger exists, so this isn't counted as an edit).
update public.posts p
set author_name = coalesce(nullif(trim(pr.full_name), ''), 'LGU Admin'),
    author_role = pr.role
from public.profiles pr
where pr.id = p.author_id and p.author_name is null;

create or replace function public.fill_post_author()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
    editor_name text;
begin
    if tg_op = 'INSERT' then
        select coalesce(nullif(trim(full_name), ''), 'LGU Admin'), role
        into new.author_name, new.author_role
        from public.profiles where id = new.author_id;
        new.edited_by_name := null;
        new.edited_at := null;
    else
        -- Authorship never changes on edit.
        new.author_name := old.author_name;
        new.author_role := old.author_role;
        new.edited_by_name := old.edited_by_name;
        new.edited_at := old.edited_at;
        -- Only a real edit by an admin counts (not e.g. the author's profile being deleted).
        if auth.uid() is not null and public.is_admin()
           and (new.title, new.body, new.type, new.pinned, new.published_at)
               is distinct from (old.title, old.body, old.type, old.pinned, old.published_at)
        then
            select coalesce(nullif(trim(full_name), ''), 'LGU Admin') into editor_name
            from public.profiles where id = auth.uid();
            new.edited_by_name := editor_name;
            new.edited_at := now();
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists posts_fill_author on public.posts;
create trigger posts_fill_author before insert or update on public.posts
    for each row execute function public.fill_post_author();

-- ─── Report timeline: who made each update ───────────────────────────────────
alter table public.report_updates
    add column if not exists author_name text,
    add column if not exists author_role public.user_role;

update public.report_updates u
set author_name = coalesce(nullif(trim(pr.full_name), ''), 'LGU staff'),
    author_role = pr.role
from public.profiles pr
where pr.id = u.author_id and u.author_name is null;

create or replace function public.fill_report_update_author()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    select coalesce(nullif(trim(full_name), ''), 'LGU staff'), role
    into new.author_name, new.author_role
    from public.profiles where id = new.author_id;
    return new;
end;
$$;

drop trigger if exists report_updates_fill_author on public.report_updates;
create trigger report_updates_fill_author before insert on public.report_updates
    for each row execute function public.fill_report_update_author();

-- ─── Comments: the author's role (name was already copied in 0007) ───────────
alter table public.post_comments add column if not exists author_role public.user_role;

update public.post_comments c
set author_role = pr.role
from public.profiles pr
where pr.id = c.user_id and c.author_role is null;

create or replace function public.fill_comment_author()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if (select count(*) from public.post_comments
        where user_id = new.user_id and created_at > now() - interval '1 hour') >= 30 then
        raise exception 'Too many comments in the last hour. Please try again later.' using errcode = 'P0001';
    end if;
    select coalesce(nullif(trim(full_name), ''), 'Resident'), role
    into new.author_name, new.author_role
    from public.profiles where id = new.user_id;
    new.author_name := coalesce(new.author_name, 'Resident');
    return new;
end;
$$;

-- ─── "Seen by": staff/admins who opened a report ─────────────────────────────
create table if not exists public.report_views (
    report_id     uuid not null references public.reports (id) on delete cascade,
    viewer_id     uuid not null references public.profiles (id) on delete cascade,
    viewer_name   text not null,
    viewer_role   public.user_role not null,
    first_seen_at timestamptz not null default now(),
    last_seen_at  timestamptz not null default now(),
    primary key (report_id, viewer_id)
);
create index if not exists report_views_report_idx on public.report_views (report_id, first_seen_at);

revoke all on public.report_views from anon, authenticated;
grant select on public.report_views to authenticated;
alter table public.report_views enable row level security;

-- Staff and admins can see who opened a report; residents can't.
create policy "Staff and admins see who viewed a report"
    on public.report_views for select to authenticated
    using (public.is_staff());

-- The only way to write: staff/admins mark a report as seen by themselves. No-op for citizens.
create or replace function public.mark_report_seen(target_report uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    me public.profiles%rowtype;
begin
    select * into me from public.profiles where id = auth.uid();
    if me.id is null or me.role not in ('staff', 'admin') then
        return;
    end if;
    insert into public.report_views (report_id, viewer_id, viewer_name, viewer_role)
    values (target_report, me.id, coalesce(nullif(trim(me.full_name), ''), 'LGU staff'), me.role)
    on conflict (report_id, viewer_id) do update
        set last_seen_at = now(),
            viewer_name = excluded.viewer_name,
            viewer_role = excluded.viewer_role;
end;
$$;

revoke all on function public.mark_report_seen(uuid) from public, anon;
grant execute on function public.mark_report_seen(uuid) to authenticated;

alter publication supabase_realtime add table public.report_views;
