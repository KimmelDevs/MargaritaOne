-- Run after 0001_init.sql.

-- Copy the signup email into profiles so admins can identify users on the Users screen.
-- Users cannot edit it (no column grant); it is set only by the signup trigger.
alter table public.profiles add column if not exists email text;

update public.profiles p
set email = u.email
from auth.users u
where u.id = p.id and p.email is null;

create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, full_name, email)
    values (new.id, new.raw_user_meta_data ->> 'full_name', new.email);
    return new;
end;
$$;

-- A report may only point at a photo inside the reporter's own storage folder.
drop policy if exists "Users submit reports as themselves" on public.reports;

create policy "Users submit reports as themselves"
    on public.reports for insert to authenticated
    with check (
        reporter_id = auth.uid()
        and status = 'pending'
        and (photo_path is null or photo_path like auth.uid()::text || '/%')
    );

-- Basic abuse protection: at most 10 reports per user per hour.
create or replace function public.limit_report_rate()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    if (
        select count(*) from public.reports
        where reporter_id = new.reporter_id
          and created_at > now() - interval '1 hour'
    ) >= 10 then
        raise exception 'Too many reports in the last hour. Please try again later.'
            using errcode = 'P0001';
    end if;
    return new;
end;
$$;

drop trigger if exists reports_rate_limit on public.reports;
create trigger reports_rate_limit
    before insert on public.reports
    for each row execute function public.limit_report_rate();
