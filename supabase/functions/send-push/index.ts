// send-push: turns database events (from migration 0009's triggers) into Firebase Cloud Messaging notifications.
//
// Secrets (Supabase dashboard -> Edge Functions -> Secrets):
//   FIREBASE_SERVICE_ACCOUNT  the whole JSON key of a Firebase service account (never put it in the app or repo)
//   PUSH_WEBHOOK_SECRET       same random string as the `push_webhook_secret` Vault entry
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase automatically.
//
// Deploy with JWT verification OFF (the database calls it with the shared secret instead):
//   supabase functions deploy send-push --no-verify-jwt

import { createClient } from "jsr:@supabase/supabase-js@2";

const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "{}");
const webhookSecret = Deno.env.get("PUSH_WEBHOOK_SECRET") ?? "";
const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

const STATUS_LABELS: Record<string, string> = {
  pending: "Pending",
  in_progress: "In progress",
  resolved: "Resolved",
  rejected: "Rejected",
  cancelled: "Cancelled",
};
const CATEGORY_LABELS: Record<string, string> = {
  road: "Road and infrastructure",
  garbage: "Garbage collection",
  streetlight: "Streetlight",
  peace_order: "Peace and order",
  other: "Other",
};
const POST_LABELS: Record<string, string> = {
  announcement: "New announcement",
  ordinance: "New ordinance",
  event: "New event",
  emergency: "Emergency alert",
};

type Push = { title: string; body: string; channel: string; data?: Record<string, string>; urgent?: boolean };

// ─── Google OAuth access token from the service account (cached ~50 min) ───────
let cachedToken: { value: string; expires: number } | null = null;

function base64url(bytes: Uint8Array | string): string {
  const b = typeof bytes === "string" ? new TextEncoder().encode(bytes) : bytes;
  let s = "";
  for (const x of b) s += String.fromCharCode(x);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function accessToken(): Promise<string> {
  if (cachedToken && cachedToken.expires > Date.now()) return cachedToken.value;
  const now = Math.floor(Date.now() / 1000);
  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64url(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = (serviceAccount.private_key as string)
    .replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`)),
  );
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claims}.${base64url(signature)}`,
    }),
  });
  if (!res.ok) throw new Error(`Google token request failed: ${res.status} ${await res.text()}`);
  const json = await res.json();
  cachedToken = { value: json.access_token, expires: Date.now() + 50 * 60 * 1000 };
  return cachedToken.value;
}

// ─── Sending ─────────────────────────────────────────────────────────────────
async function send(target: { token: string } | { topic: string }, push: Push): Promise<boolean> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          ...target,
          // Data-only, so the app builds the notification (and its tap action) itself.
          data: { title: push.title, body: push.body, channel: push.channel, ...(push.data ?? {}) },
          android: { priority: push.urgent ? "high" : "normal" },
        },
      }),
    },
  );
  if (res.ok) return true;
  const text = await res.text();
  // The app was uninstalled or the token expired: forget it.
  if ("token" in target && (res.status === 404 || text.includes("UNREGISTERED"))) {
    await supabase.from("device_tokens").delete().eq("token", target.token);
  } else {
    console.error("FCM send failed", res.status, text);
  }
  return false;
}

async function sendToUsers(userIds: string[], push: Push) {
  if (userIds.length === 0) return 0;
  const { data, error } = await supabase.from("device_tokens").select("token").in("user_id", userIds);
  if (error) throw error;
  const results = await Promise.allSettled((data ?? []).map((row) => send({ token: row.token }, push)));
  return results.filter((r) => r.status === "fulfilled" && r.value).length;
}

// ─── Events ──────────────────────────────────────────────────────────────────
async function onReportUpdate(record: Record<string, string | null>) {
  const { data: report } = await supabase
    .from("reports").select("id, reporter_id, title").eq("id", record.report_id).single();
  if (!report || report.reporter_id === record.author_id) return 0;
  const body = record.status
    ? `"${report.title}" is now ${STATUS_LABELS[record.status] ?? record.status}.`
    : `New note from the LGU on "${report.title}": ${record.note ?? ""}`.trim();
  return sendToUsers([report.reporter_id], {
    title: "Update on your report",
    body,
    channel: "reports",
    data: { report_id: report.id },
  });
}

async function onNewReport(record: Record<string, string>) {
  const { data: staff, error } = await supabase.from("profiles").select("id").in("role", ["staff", "admin"]);
  if (error) throw error;
  const ids = (staff ?? []).map((p) => p.id).filter((id) => id !== record.reporter_id);
  return sendToUsers(ids, {
    title: "New report",
    body: `${CATEGORY_LABELS[record.category] ?? "Report"}: ${record.title}`,
    channel: "reports",
    data: { report_id: record.id },
  });
}

async function onNewPost(record: Record<string, string>) {
  const urgent = record.type === "emergency";
  const ok = await send({ topic: "bulletin" }, {
    title: POST_LABELS[record.type] ?? "New post",
    body: record.title,
    channel: urgent ? "emergency" : "bulletin",
    urgent,
    data: { post_id: record.id },
  });
  return ok ? 1 : 0;
}

Deno.serve(async (req) => {
  if (!webhookSecret || req.headers.get("x-push-secret") !== webhookSecret) {
    return new Response("Unauthorized", { status: 401 });
  }
  try {
    const { event, record } = await req.json();
    const sent = event === "report_update" ? await onReportUpdate(record)
      : event === "new_report" ? await onNewReport(record)
      : event === "new_post" ? await onNewPost(record)
      : 0;
    return Response.json({ event, sent });
  } catch (e) {
    console.error(e);
    return new Response(String(e), { status: 500 });
  }
});
