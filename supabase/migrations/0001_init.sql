-- MyGandara initial schema.
-- Run in the Supabase dashboard SQL editor (or `supabase db push`) on a fresh project.

-- ─── Types ───────────────────────────────────────────────────────────────────

create type public.user_role as enum ('citizen', 'staff', 'admin');
create type public.report_status as enum ('pending', 'in_progress', 'resolved', 'rejected');
create type public.report_category as enum ('road', 'garbage', 'streetlight', 'peace_order', 'other');
create type public.post_type as enum ('announcement', 'ordinance', 'event', 'emergency');

-- ─── Tables ──────────────────────────────────────────────────────────────────

create table public.profiles (
    id          uuid primary key references auth.users (id) on delete cascade,
    full_name   text,
    phone       text,
    barangay    text,
    role        public.user_role not null default 'citizen',
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table public.reports (
    id           uuid primary key default gen_random_uuid(),
    reporter_id  uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    title        text not null check (char_length(title) between 3 and 120),
    description  text not null default '' check (char_length(description) <= 2000),
    category     public.report_category not null,
    status       public.report_status not null default 'pending',
    photo_path   text,             -- object path inside the report-photos bucket
    latitude     double precision check (latitude between -90 and 90),
    longitude    double precision check (longitude between -180 and 180),
    address      text,
    assigned_to  uuid references public.profiles (id) on delete set null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index reports_reporter_id_idx on public.reports (reporter_id);
create index reports_status_idx on public.reports (status);

create table public.report_updates (
    id          uuid primary key default gen_random_uuid(),
    report_id   uuid not null references public.reports (id) on delete cascade,
    author_id   uuid default auth.uid() references public.profiles (id) on delete set null,
    status      public.report_status,   -- null = note only, no status change
    note        text check (char_length(note) <= 1000),
    created_at  timestamptz not null default now()
);

create index report_updates_report_id_idx on public.report_updates (report_id, created_at);

create table public.posts (
    id               uuid primary key default gen_random_uuid(),
    author_id        uuid default auth.uid() references public.profiles (id) on delete set null,
    type             public.post_type not null default 'announcement',
    title            text not null check (char_length(title) between 3 and 160),
    body             text not null default '',
    attachment_path  text,         -- object path inside the post-attachments bucket
    pinned           boolean not null default false,
    published_at     timestamptz not null default now(),
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

create index posts_published_at_idx on public.posts (pinned desc, published_at desc);

-- ─── Helper functions (security definer so RLS policies can read roles) ──────

create or replace function public.current_user_role()
returns public.user_role
language sql stable security definer set search_path = ''
as $$
    select role from public.profiles where id = auth.uid()
$$;

create or replace function public.is_staff()
returns boolean
language sql stable security definer set search_path = ''
as $$
    select coalesce(public.current_user_role() in ('staff', 'admin'), false)
$$;

create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = ''
as $$
    select coalesce(public.current_user_role() = 'admin', false)
$$;

-- ─── Triggers ────────────────────────────────────────────────────────────────

-- Every new auth user gets a profile with role 'citizen'. Users never pick their role.
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, full_name)
    values (new.id, new.raw_user_meta_data ->> 'full_name');
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger profiles_touch before update on public.profiles
    for each row execute function public.touch_updated_at();
create trigger reports_touch before update on public.reports
    for each row execute function public.touch_updated_at();
create trigger posts_touch before update on public.posts
    for each row execute function public.touch_updated_at();

-- First timeline entry when a report is submitted.
create or replace function public.handle_new_report()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.report_updates (report_id, author_id, status, note)
    values (new.id, new.reporter_id, 'pending', 'Report received');
    return new;
end;
$$;

create trigger on_report_created
    after insert on public.reports
    for each row execute function public.handle_new_report();

-- Staff change a report's status by adding a timeline entry; this keeps reports.status in sync.
create or replace function public.apply_report_update()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if new.status is not null then
        update public.reports set status = new.status where id = new.report_id;
    end if;
    return new;
end;
$$;

create trigger on_report_update_created
    after insert on public.report_updates
    for each row execute function public.apply_report_update();

-- Admin-only role management. The only way to change a role from the app.
create or replace function public.set_user_role(target_user uuid, new_role public.user_role)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if not public.is_admin() then
        raise exception 'Only admins can change roles' using errcode = '42501';
    end if;
    if target_user = auth.uid() then
        raise exception 'Admins cannot change their own role' using errcode = '42501';
    end if;
    update public.profiles set role = new_role where id = target_user;
end;
$$;

revoke execute on function public.set_user_role(uuid, public.user_role) from public, anon;
grant execute on function public.set_user_role(uuid, public.user_role) to authenticated;

-- ─── Table privileges (column-level, on top of RLS) ──────────────────────────
-- Supabase grants everything to anon/authenticated by default; narrow it down.

revoke all on public.profiles, public.reports, public.report_updates, public.posts from anon, authenticated;

grant select on public.profiles to authenticated;
grant update (full_name, phone, barangay) on public.profiles to authenticated;  -- never `role`

grant select on public.reports to authenticated;
grant insert (title, description, category, photo_path, latitude, longitude, address) on public.reports to authenticated;
grant update (assigned_to) on public.reports to authenticated;

grant select on public.report_updates to authenticated;
grant insert (report_id, status, note) on public.report_updates to authenticated;

grant select on public.posts to anon, authenticated;
grant insert (type, title, body, attachment_path, pinned, published_at) on public.posts to authenticated;
grant update (type, title, body, attachment_path, pinned, published_at) on public.posts to authenticated;
grant delete on public.posts to authenticated;

-- ─── Row Level Security ──────────────────────────────────────────────────────

alter table public.profiles enable row level security;
alter table public.reports enable row level security;
alter table public.report_updates enable row level security;
alter table public.posts enable row level security;

-- profiles
create policy "Users read own profile; staff read all"
    on public.profiles for select to authenticated
    using (id = auth.uid() or public.is_staff());

create policy "Users update own profile"
    on public.profiles for update to authenticated
    using (id = auth.uid()) with check (id = auth.uid());

-- reports
create policy "Citizens read own reports; staff read all"
    on public.reports for select to authenticated
    using (reporter_id = auth.uid() or public.is_staff());

create policy "Users submit reports as themselves"
    on public.reports for insert to authenticated
    with check (reporter_id = auth.uid() and status = 'pending');

create policy "Staff update reports"
    on public.reports for update to authenticated
    using (public.is_staff()) with check (public.is_staff());

-- report_updates
create policy "Timeline visible to report owner and staff"
    on public.report_updates for select to authenticated
    using (
        public.is_staff()
        or exists (select 1 from public.reports r where r.id = report_id and r.reporter_id = auth.uid())
    );

create policy "Staff add timeline entries"
    on public.report_updates for insert to authenticated
    with check (public.is_staff() and author_id = auth.uid());

-- posts
create policy "Anyone reads published posts; admins read all"
    on public.posts for select to anon, authenticated
    using (published_at <= now() or public.is_admin());

create policy "Admins create posts"
    on public.posts for insert to authenticated
    with check (public.is_admin() and author_id = auth.uid());

create policy "Admins edit posts"
    on public.posts for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy "Admins delete posts"
    on public.posts for delete to authenticated
    using (public.is_admin());

-- ─── Storage ─────────────────────────────────────────────────────────────────

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
    ('report-photos', 'report-photos', false, 5242880, array['image/jpeg', 'image/png', 'image/webp']),
    ('post-attachments', 'post-attachments', true, 10485760, array['image/jpeg', 'image/png', 'image/webp', 'application/pdf']);

-- report-photos: private. Uploads go under "<user id>/<file>"; owner and staff can read.
create policy "Users upload report photos to own folder"
    on storage.objects for insert to authenticated
    with check (bucket_id = 'report-photos' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "Owner and staff read report photos"
    on storage.objects for select to authenticated
    using (
        bucket_id = 'report-photos'
        and ((storage.foldername(name))[1] = auth.uid()::text or public.is_staff())
    );

-- post-attachments: public read via public URL; only admins write.
create policy "Admins upload post attachments"
    on storage.objects for insert to authenticated
    with check (bucket_id = 'post-attachments' and public.is_admin());

create policy "Admins update post attachments"
    on storage.objects for update to authenticated
    using (bucket_id = 'post-attachments' and public.is_admin());

create policy "Admins delete post attachments"
    on storage.objects for delete to authenticated
    using (bucket_id = 'post-attachments' and public.is_admin());

-- ─── Realtime ────────────────────────────────────────────────────────────────

alter publication supabase_realtime add table public.reports, public.report_updates, public.posts;
