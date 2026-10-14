-- Run after 0005. Lets admins manage hotline categories (name, icon, order, urgent colour) instead of a fixed list.

create table if not exists public.hotline_categories (
    id          uuid primary key default gen_random_uuid(),
    name        text not null unique check (char_length(name) between 2 and 60),
    -- One of the icons the app knows; unknown values fall back to a phone icon.
    icon        text not null default 'phone',
    is_urgent   boolean not null default false,   -- shown in red, like the national emergency line
    sort_order  int not null default 100,
    created_at  timestamptz not null default now()
);

revoke all on public.hotline_categories from anon, authenticated;
grant select on public.hotline_categories to anon, authenticated;
grant insert (name, icon, is_urgent, sort_order) on public.hotline_categories to authenticated;
grant update (name, icon, is_urgent, sort_order) on public.hotline_categories to authenticated;
grant delete on public.hotline_categories to authenticated;

alter table public.hotline_categories enable row level security;

create policy "Anyone reads hotline categories"
    on public.hotline_categories for select to anon, authenticated using (true);
create policy "Admins add hotline categories"
    on public.hotline_categories for insert to authenticated with check (public.is_admin());
create policy "Admins edit hotline categories"
    on public.hotline_categories for update to authenticated
    using (public.is_admin()) with check (public.is_admin());
create policy "Admins delete hotline categories"
    on public.hotline_categories for delete to authenticated using (public.is_admin());

alter publication supabase_realtime add table public.hotline_categories;

-- Start with the categories the app used to have built in. English names double as translation keys in the app.
insert into public.hotline_categories (name, icon, is_urgent, sort_order) values
    ('Emergency', 'warning', true, 0),
    ('Fire', 'fire', false, 10),
    ('Police', 'police', false, 20),
    ('Hospital / medical', 'medical', false, 30),
    ('Disaster response', 'flood', false, 40),
    ('Water / power', 'power', false, 50),
    ('Other', 'phone', false, 100)
on conflict (name) do nothing;

-- Link hotlines to a category row. Deleting a category leaves its hotlines uncategorised.
alter table public.emergency_contacts
    add column if not exists category_id uuid references public.hotline_categories (id) on delete set null;

update public.emergency_contacts c
set category_id = hc.id
from public.hotline_categories hc
where c.category_id is null
  and hc.name = case c.category
      when 'emergency' then 'Emergency'
      when 'fire' then 'Fire'
      when 'police' then 'Police'
      when 'medical' then 'Hospital / medical'
      when 'disaster' then 'Disaster response'
      when 'utility' then 'Water / power'
      else 'Other'
  end;

-- The old fixed-list column is no longer used by the app.
alter table public.emergency_contacts drop constraint if exists emergency_contacts_category_check;
grant insert (category_id) on public.emergency_contacts to authenticated;
grant update (category_id) on public.emergency_contacts to authenticated;
