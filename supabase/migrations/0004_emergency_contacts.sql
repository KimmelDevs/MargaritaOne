-- Run after 0003. Emergency hotlines directory: everyone (even signed out) can read; only admins edit.

create table if not exists public.emergency_contacts (
    id          uuid primary key default gen_random_uuid(),
    name        text not null check (char_length(name) between 2 and 120),
    category    text not null default 'other'
                check (category in ('emergency', 'fire', 'police', 'medical', 'disaster', 'utility', 'other')),
    phone       text not null check (char_length(phone) between 3 and 40),
    note        text check (char_length(note) <= 200),   -- e.g. "24/7", "Brgy. Rizal"
    sort_order  int not null default 100,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create trigger emergency_contacts_touch before update on public.emergency_contacts
    for each row execute function public.touch_updated_at();

revoke all on public.emergency_contacts from anon, authenticated;
grant select on public.emergency_contacts to anon, authenticated;
grant insert (name, category, phone, note, sort_order) on public.emergency_contacts to authenticated;
grant update (name, category, phone, note, sort_order) on public.emergency_contacts to authenticated;
grant delete on public.emergency_contacts to authenticated;

alter table public.emergency_contacts enable row level security;

create policy "Anyone reads emergency contacts"
    on public.emergency_contacts for select to anon, authenticated
    using (true);

create policy "Admins add emergency contacts"
    on public.emergency_contacts for insert to authenticated
    with check (public.is_admin());

create policy "Admins edit emergency contacts"
    on public.emergency_contacts for update to authenticated
    using (public.is_admin()) with check (public.is_admin());

create policy "Admins delete emergency contacts"
    on public.emergency_contacts for delete to authenticated
    using (public.is_admin());

alter publication supabase_realtime add table public.emergency_contacts;

-- Nationwide hotlines. Admins add the local Gandara numbers (BFP, PNP, MDRRMO, RHU/hospital) from the app.
insert into public.emergency_contacts (name, category, phone, note, sort_order) values
    ('National Emergency Hotline', 'emergency', '911', 'Police, fire, and medical emergencies nationwide', 0),
    ('Philippine Red Cross', 'medical', '143', 'Ambulance and disaster response', 10);
