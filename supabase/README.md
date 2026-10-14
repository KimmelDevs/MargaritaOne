# Supabase setup

Everything here works on Supabase's **free plan**.

1. Create a project at https://supabase.com/dashboard (region: Southeast Asia / Singapore is closest to Samar). Save the database password somewhere safe; the app never needs it.
2. Open **SQL Editor** and run, in order, each once:
   - `migrations/0001_init.sql`
   - `migrations/0002_profile_email_and_hardening.sql`
   - `migrations/0003_cancel_and_reference_numbers.sql` (reference numbers like MG-2026-0042, and letting citizens cancel pending reports)
   - `migrations/0004_emergency_contacts.sql` (emergency hotlines; seeds 911 and Red Cross 143 — admins add the local Gandara numbers in the app)
   - `migrations/0005_post_images_and_reactions.sql` (up to 10 photos per bulletin post, and Facebook-style reactions)
   - `migrations/0006_hotline_categories.sql` (admin-managed hotline categories; moves existing hotlines into them)
   - `migrations/0007_comments_events_barangays_audit.sql` (comments, event details, barangay list, "Was this fixed?" ratings, delete-my-account, activity log)
3. **Authentication → Sign In / Providers → Email**: for testing, turn **Confirm email** off. Supabase's built-in email sender only allows a few emails per hour; before launch, either keep it off or add a free SMTP provider (Authentication → Emails → SMTP).
4. **Project Settings → API**: copy the Project URL and the `anon` public key into the root `local.properties`:

   ```
   SUPABASE_URL=https://<project-ref>.supabase.co
   SUPABASE_ANON_KEY=<anon key>
   ```

   `local.properties` is gitignored. Never put the `service_role` key, JWT secret, or DB password in the app.
5. Sync Gradle in Android Studio and run the app.

## Forgot password (one-time setup)

The app resets passwords with a 6-digit code typed into the app (no links to open). Supabase's default
"Reset password" email only contains a link, so add the code to it:

1. **Authentication → Emails → Templates → Reset Password**.
2. Add this line to the message body (keep the rest as you like):

   ```html
   <p>Your MyGandara reset code is: <strong>{{ .Token }}</strong></p>
   ```
3. Save. The built-in email sender is limited to a few emails per hour; add a free SMTP provider before launch.

## Making the first admin

New signups are always `citizen`. Sign up in the app, then run in the SQL editor:

```sql
update public.profiles set role = 'admin' where email = 'you@example.com';
```

Sign out and back in. From then on, admins change roles from the **Users** tab.

## Security model (summary)

| Data | Citizen | Staff | Admin | Signed out |
|---|---|---|---|---|
| Own profile | read, edit name/phone/barangay | same | same | — |
| Other profiles | — | read | read | — |
| Roles | — | — | change others' via `set_user_role()` | — |
| Reports | create (rate-limited: 10/hour), read own, cancel own while pending (`cancel_my_report()`) | read all, assign | same as staff | — |
| Report timeline | read own reports' | read all, add updates (status changes go through here) | same as staff | — |
| Bulletin posts | read | read | create, edit, delete | read |
| Emergency hotlines + categories | read | read | add, edit, delete | read |
| Barangay list | read | read | add, rename, delete | read |
| Post comments | read all; add, delete own | same | same + delete any (moderation) | — |
| Resolution rating | rate own resolved report (`rate_resolution()`) | read | read | — |
| Activity log | — | — | read (written only by triggers) | — |
| Own account | delete (`delete_my_account()`, not for admins) | same | must be demoted first | — |
| Post reactions | see all counts; add/change/remove own | same | same | — |
| `report-photos` bucket (private) | upload to own folder, read own | read all | read all | — |
| `post-attachments` bucket (public) | read | read | upload, delete | read |

Enforced in the database with RLS policies plus column-level grants (so nobody can set their own `role`,
`status`, or `reporter_id`). Run `tests/rls_checks.sql` after any policy change.

## Known gaps

- Deleting a user from the dashboard (Authentication → Users) doesn't remove their photos; the in-app "Delete my account" does.
- Google Play also asks for a **web** link where people can request account deletion (e.g. an LGU page or email address).
- The privacy notice (`PrivacyNoticeScreen.kt`) has a placeholder for the DPO contact details.
