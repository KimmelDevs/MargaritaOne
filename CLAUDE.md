# MargaritaOne

Citizen reporting + LGU bulletin board app for Santa Margarita, Samar (similar to MyNaga).

Started as a copy of the MyGandara app (same features and code). **For now it uses the same Supabase project as
MyGandara** (same tables, buckets, migrations already applied), so data is shared between the two apps until
MyGandara gets its own project. Barangays and hotlines live in that shared database; admins edit them in the app.

## Stack

- Kotlin + Jetpack Compose (Material 3), Navigation Compose
- Supabase via `supabase-kt`: Auth, Postgrest, Storage, Realtime
- Ktor OkHttp engine (not the "android" engine: it closes connections on the main thread when a request is cancelled, which crashes with NetworkOnMainThreadException)
- Package: `com.kimmeldevs.margaritaone`, minSdk 24, targetSdk 36, AGP 9.2.1, Kotlin 2.2.10
- Dependencies go in `gradle/libs.versions.toml`, not as inline strings in `app/build.gradle.kts`

## Roles

| Role | Powers |
|---|---|
| `citizen` | Default on signup. Submits reports, reads the bulletin. |
| `staff` | Manages reports, updates their status. |
| `admin` | Staff powers + posts to the bulletin + promotes users. |

- Role lives in a `profiles` table linked to `auth.users` and is enforced with Row Level Security.
- Users never choose their role at signup. A database trigger on `auth.users` insert creates the profile with role `'citizen'`.
- The client UI may hide/show things by role, but RLS is the real enforcement. Never rely on the app alone.

## Data model

Tables:
- `profiles` — id (= auth.users.id), name, role, etc.
- `reports` — citizen-submitted issues (category, description, status, photo, GPS)
- `report_updates` — status timeline entries for a report
- `posts` — bulletin items; `type` is one of `announcement`, `ordinance`, `event`, `emergency`

Storage buckets: `report-photos`, `post-attachments`.

Keep SQL (schema, triggers, RLS policies) in version-controlled files under `supabase/` so it can be reviewed and re-applied.

## Secrets

- Supabase URL and **anon** key go in `local.properties` (gitignored) and are read into `BuildConfig` at build time. Never hardcode or commit them.
- The service role key / JWT secret / DB password must never be in the Android app or this repo. They belong only in Supabase Edge Function secrets or server-side environments.

## Build order

Work one step at a time and make sure the project builds (`./gradlew assembleDebug`) after each step.

1. Supabase setup + SQL schema + ViewModel/repository layer
2. Auth + role-based navigation
3. Real reports with photo + GPS
4. Bulletin board posting
5. Staff tools + admin user management
6. Push notifications (FCM triggered from a Supabase Edge Function) + polish
7. Review RLS policies + privacy notice

## Current state

Steps 1–7 are implemented in code (push needs the Firebase setup in `supabase/README.md`). Builds with `./gradlew assembleDebug`
(needs `JAVA_HOME` = Android Studio's `jbr`; compileSdk 37, Kotlin 2.4.21, supabase-kt 3.8.0). Not yet tested against a
live Supabase project.

- SQL: `supabase/migrations/0001_init.sql`, `0002_profile_email_and_hardening.sql`, `0003_cancel_and_reference_numbers.sql`, `0004_emergency_contacts.sql`, `0005_post_images_and_reactions.sql`, `0006_hotline_categories.sql`, `0007_comments_events_barangays_audit.sql`, `0008_accountability.sql`, `0009_push_notifications.sql`, `0010_officials.sql`; checks in `supabase/tests/rls_checks.sql`;
  setup + security model in `supabase/README.md`. Add new SQL as new numbered migration files, never edit applied ones.
- Status changes go through `report_updates` (trigger copies status onto `reports`). Roles change only via the admin-only
  `set_user_role()` RPC. Reports are rate-limited to 10/hour per user.
- `data/`: `SupabaseProvider` (lazy client), `AppContainer` (manual DI held by `MargaritaOneApp`), DTOs in `data/model/`,
  repositories (Auth, Profile, Report, Post, Realtime).
- `ui/viewmodel/`: one ViewModel per screen, created with `appViewModelFactory`. `Loadable` + `UiStateContent` handle
  loading/error/pull-to-refresh. `SessionViewModel` drives `navigation/NavGraph.kt` (`MargaritaOneRoot`): signed-out graph
  (login, sign-up, privacy) vs signed-in graph with role-based bottom tabs (`Tab.forRole`).
- Photos: `util/ImageCompressor` (resize + strip EXIF). GPS: `util/LocationHelper` (fused location, asked on tap).
- Privacy notice (`ui/screens/privacy/PrivacyNoticeScreen.kt`): contact is the Office of the Municipal Mayor until the
  LGU names a Data Protection Officer (none publicly listed).
- Branding (design kit, Oct 2026): palette in `ui/theme/Color.kt` (Primary Red #E31837 actions/alerts, Primary Blue
  #0866C6 links/nav, Deep Navy #0D3157 headings, Cool White #F4F7FB background, white cards) with `BrandGradient`
  (red→blue) for primary buttons. Shared pieces in `ui/components/Brand.kt`: `MargaritaOneLogo`/`Wordmark` (M logo
  `logo_m.png` + "Margarita" navy / "One" red), `GradientButton`, `IconBadge`, `BrandWaves`, `SectionHeader`.
  Launcher icon = M logo; crab `mascot.png` is the login illustration (`login_hero.jpg`) and notification large icon.
  Home tab (`ui/screens/home/`): greeting carousel (`hero_town.jpg`, `hero_welcome.jpg`), six role-based shortcut tiles
  (only features that exist), latest bulletin posts. Tabs: citizen Home/Bulletin/My reports/Hotlines/Profile; staff
  Home/Reports/Bulletin/Hotlines/Profile; admin Home/Reports/Bulletin/Users/Profile (Hotlines via a Home tile).
  Dynamic colour is off on purpose. Map opens at `TOWN_CENTER` (Santa Margarita poblacion) in `ui/components/TownMap.kt`.
- UI text is English wrapped in `t("...")` (`i18n/I18n.kt`); the English text is the key into
  `i18n/FilipinoStrings.kt` and `i18n/WarayStrings.kt` (missing keys fall back to English). When adding UI text,
  wrap it in `t()` and add both translations. Templates use `%s` / `%1$s` (escape `$` in Kotlin). Shared components
  (ConfirmDialog, MessageBox, EmptyListText, ChoiceChipRow, StatusBadge, PillBadge) translate their inputs themselves.
  `TranslationsTest` checks placeholders match. The Waray table is a first draft that needs native-speaker review.
- Hotlines tab (`ui/screens/hotlines/`): `emergency_contacts` + admin-managed `hotline_categories` (name, icon key from
  `HotlineIcon`, urgent flag, order; seeded English names double as translation keys). Readable signed-out too
  (button on Login); both cached in LocalStore for offline. Cards expand on tap to show Call (ACTION_DIAL, so the
  dialer is a second step) and Copy number — no call button on collapsed cards, to avoid accidental calls.
- Profile -> "Posts I reacted to" reuses `BulletinScreen(reactedOnly = true)`.
- Posts: create/edit share `CreatePostScreen` (`create_post?postId=`); `published_at` in the future = scheduled
  (RLS hides it from non-admins); event posts have `event_start/end/location` + "Add to calendar". Deleting/editing
  removes orphaned files from storage. Comments: `post_comments` (+ `CommentsSheet`); author name copied by trigger.
- Maps: MapLibre Android + free OpenFreeMap tiles (`TownMap`); reports list/map toggle (`ReportMapContent`),
  `LocationPickerDialog` on Create report. (mapcn is React/web-only; same MapLibre engine.)
- Offline: reports that fail for network reasons go to `data/Outbox` and `OutboxWorker` (WorkManager) sends them
  when online; banner on My reports.
- Forgot password = 6-digit email code (`verifyEmailOtp` RECOVERY); needs `{{ .Token }}` in the Supabase template.
- Admin: Profile -> Manage barangays (`barangays` table, used by reports/profiles), Activity log (`audit_log`,
  written only by triggers). Dashboard overflow -> Export CSV of the filtered list (column names stay English).
- Delete my account: removes own report photos, then `delete_my_account()` RPC (refused for admins).
- Bulletin posts: `image_paths` (up to 10, `PostImageGrid` Facebook layout + `ImageGalleryDialog` swipe/zoom viewer);
  `attachment_path` is for a PDF (old posts may have an image there; `PostDto.allImagePaths` merges them).
  Reactions in `post_reactions` (one per user per post, `ReactionBar`: tap = Like/remove, long-press = picker),
  updated optimistically in `BulletinViewModel.react`.
- Theme: System/Light/Dark chosen in Profile (`ThemeModePicker`), provided via `LocalThemeMode` from MainActivity.
- On-device prefs (`util/LocalStore`): language, theme mode, cached hotlines, seen report versions ("Updated" dot), dismissed emergency banner.
- Offline: `Loadable(scope, localStore.offlineCache("key"))` shows the last good copy (JSON in `filesDir/offline`) and
  keeps it on screen when a refresh fails. Cached: bulletin (+ reactions/comment counts), report lists, each opened
  report, emergency banner, barangays (in `DirectoryRepository`), and the profile (`SessionViewModel` opens the app
  with it when offline, including Supabase's `RefreshFailure` for an expired token). `Loadable` waits on
  `SupabaseProvider.awaitSessionReady()` so it never queries as signed-out. `clearAccountData()` on sign-out.
- Accountability (0008): posts carry `author_name/author_role` + `edited_by_name/edited_at`, timeline entries and
  comments carry the author name/role (all filled by triggers; clients can't write them). `RoleBadge`/`AuthorLine`
  show an Admin shield / Staff ID pill. `report_views` records staff/admins opening a report via the
  `mark_report_seen()` RPC (once per visit in `ReportDetailViewModel`); readable by staff/admins only: "Seen by"
  section on report detail and a "Seen" eye on the dashboard. Residents never see it.
- Push (0009 + `supabase/functions/send-push`): `device_tokens` written only via `register_device_token` /
  `unregister_device_token` RPCs; triggers call the Edge Function with pg_net (URL + shared secret from Vault; no-op
  until set, never blocks inserts). The function holds the Firebase service account (Edge secret only) and sends
  data-only FCM messages: report updates -> reporter, new reports -> staff/admins, posts -> topic `bulletin`.
  App: `push/` (`Push.init` from `MargaritaOneApp`, FirebaseApp built from `FIREBASE_*` in local.properties, no
  google-services plugin; `PushService` shows notifications; `PushTarget` opens the report/bulletin on tap).
  Register on sign-in and unregister before sign-out in `SessionViewModel`. Notification permission asked once.
- About screen (`ui/screens/about/`, from Login and Profile): town history/facts and `officials` (0010; anyone reads,
  admins edit in-app incl. photos uploaded to `post-attachments/officials/`). Seeded with the 2025–2028 officials from
  public sources; the LGU should confirm. `supabase/seed/santa_margarita_barangays.sql` loads the 36 barangays — run
  only once the database no longer serves MyGandara.
