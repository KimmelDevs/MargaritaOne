-- Manual RLS checks. Paste into the SQL editor and run section by section.
-- Replace the three emails with real test accounts (one citizen, one staff, one admin).
-- Everything runs inside a transaction that is rolled back, so nothing is changed.

begin;

-- ─── Act as the CITIZEN ──────────────────────────────────────────────────────
select set_config('request.jwt.claims', json_build_object(
    'sub', (select id from auth.users where email = 'citizen@example.com'),
    'role', 'authenticated')::text, true);
set local role authenticated;

select count(*) as profiles_visible_expect_1 from public.profiles;
select count(*) as other_peoples_reports_expect_0 from public.reports where reporter_id <> auth.uid();

-- Each of these must FAIL with a permission / RLS error. Run them one at a time
-- (a failure aborts the transaction; run `rollback; begin;` and the setup above again).
-- update public.profiles set role = 'admin' where id = auth.uid();            -- column not granted
-- insert into public.posts (title, body) values ('hack', 'x');                -- admins only
-- insert into public.report_updates (report_id, status) select id, 'resolved' from public.reports limit 1;  -- staff only
-- insert into public.reports (title, category, photo_path) values ('Test', 'road', 'someone-else/x.jpg');   -- not own folder
-- select public.set_user_role(auth.uid(), 'admin');                           -- admins only

reset role;

-- ─── Act as STAFF ────────────────────────────────────────────────────────────
select set_config('request.jwt.claims', json_build_object(
    'sub', (select id from auth.users where email = 'staff@example.com'),
    'role', 'authenticated')::text, true);
set local role authenticated;

select count(*) as all_reports_visible from public.reports;
select count(*) as all_profiles_visible from public.profiles;
-- Must FAIL:
-- insert into public.posts (title, body) values ('Staff post', 'x');          -- admins only

reset role;

-- ─── Act as ANON (signed out) ────────────────────────────────────────────────
set local role anon;
select count(*) as posts_visible_to_public from public.posts;
-- Must FAIL:
-- select * from public.reports;
-- select * from public.profiles;

rollback;
