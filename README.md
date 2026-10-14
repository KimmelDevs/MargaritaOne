# MargaritaOne

Citizen reporting and LGU bulletin board app for Santa Margarita, Samar.
Residents report problems with a photo and location, follow their status, read official announcements,
and call emergency hotlines (works offline). Staff and admins manage reports, posts, users and hotlines.

Kotlin + Jetpack Compose, Supabase (Auth, Postgres with row-level security, Storage, Realtime).

## Setup

1. Add your Supabase project to `local.properties` (never commit this file):
   ```
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   ```
2. Run the SQL in `supabase/migrations/` in order (see `supabase/README.md`).
3. Open in Android Studio and run, or build with `./gradlew assembleDebug`.
