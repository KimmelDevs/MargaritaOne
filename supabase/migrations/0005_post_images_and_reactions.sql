-- Run after 0004. Multiple images per bulletin post, and Facebook-style reactions.

-- ─── Multiple images ─────────────────────────────────────────────────────────
-- Object paths inside the post-attachments bucket, shown as a photo grid.
-- attachment_path stays for a single PDF (older posts may have an image there too; the app shows both).
alter table public.posts
    add column if not exists image_paths text[] not null default '{}'
    check (cardinality(image_paths) <= 10);

grant insert (image_paths) on public.posts to authenticated;
grant update (image_paths) on public.posts to authenticated;

-- ─── Reactions ───────────────────────────────────────────────────────────────
do $$ begin
    create type public.reaction_type as enum ('like', 'love', 'care', 'haha', 'wow', 'sad', 'angry');
exception when duplicate_object then null;
end $$;

-- One reaction per user per post (changing it updates the row, like Facebook).
create table if not exists public.post_reactions (
    post_id     uuid not null references public.posts (id) on delete cascade,
    user_id     uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    reaction    public.reaction_type not null,
    created_at  timestamptz not null default now(),
    primary key (post_id, user_id)
);

create index if not exists post_reactions_post_id_idx on public.post_reactions (post_id);

revoke all on public.post_reactions from anon, authenticated;
grant select on public.post_reactions to authenticated;
grant insert (post_id, reaction) on public.post_reactions to authenticated;
grant update (reaction) on public.post_reactions to authenticated;
grant delete on public.post_reactions to authenticated;

alter table public.post_reactions enable row level security;

-- Counts are visible to every signed-in user; only your own reaction can be added, changed, or removed.
create policy "Signed-in users see reactions"
    on public.post_reactions for select to authenticated
    using (true);

create policy "Users react as themselves"
    on public.post_reactions for insert to authenticated
    with check (user_id = auth.uid());

create policy "Users change their own reaction"
    on public.post_reactions for update to authenticated
    using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy "Users remove their own reaction"
    on public.post_reactions for delete to authenticated
    using (user_id = auth.uid());

alter publication supabase_realtime add table public.post_reactions;
