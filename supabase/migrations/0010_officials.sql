-- 0010: municipal officials for the "About Santa Margarita" screen.
-- Readable by everyone (also signed out); only admins add, edit or remove. Photos are uploaded by admins from the
-- app into the public post-attachments bucket under officials/ (that bucket is already admin-write only).
--
-- Seeded with the 2025–2028 officials as listed publicly (Wikipedia / Rappler 2025 results). The LGU should
-- confirm the names and spelling; admins can correct them in the app.

create table if not exists public.officials (
    id          uuid primary key default gen_random_uuid(),
    name        text not null check (char_length(name) between 2 and 120),
    position    text not null check (char_length(position) between 2 and 80),
    photo_path  text,          -- object path inside the post-attachments bucket
    sort_order  int not null default 100,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

drop trigger if exists officials_touch on public.officials;
create trigger officials_touch before update on public.officials
    for each row execute function public.touch_updated_at();

revoke all on public.officials from anon, authenticated;
grant select on public.officials to anon, authenticated;
grant insert (name, position, photo_path, sort_order) on public.officials to authenticated;
grant update (name, position, photo_path, sort_order) on public.officials to authenticated;
grant delete on public.officials to authenticated;
alter table public.officials enable row level security;

create policy "Anyone reads officials" on public.officials for select to anon, authenticated using (true);
create policy "Admins add officials" on public.officials for insert to authenticated with check (public.is_admin());
create policy "Admins edit officials" on public.officials for update to authenticated
    using (public.is_admin()) with check (public.is_admin());
create policy "Admins remove officials" on public.officials for delete to authenticated using (public.is_admin());

-- Only seed an empty table, so re-running never duplicates.
insert into public.officials (name, position, sort_order)
select v.name, v.position, v.sort_order
from (values
    ('Felix Roma Panganoron', 'Mayor', 1),
    ('Soledad Perez Deloraya', 'Vice Mayor', 2),
    ('Alma A. Alegria', 'Councilor', 10),
    ('Jesus C. Egot', 'Councilor', 11),
    ('Winefreda D. Relos', 'Councilor', 12),
    ('Edwin A. Pueblos', 'Councilor', 13),
    ('Ma. Josefina A. Escuadra', 'Councilor', 14),
    ('Maria M. Salomon', 'Councilor', 15),
    ('Hermie C. Mendoza', 'Councilor', 16),
    ('Ma. Liza J. Pedrosa', 'Councilor', 17)
) as v(name, position, sort_order)
where not exists (select 1 from public.officials);

alter publication supabase_realtime add table public.officials;
