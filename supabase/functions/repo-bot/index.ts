// GitHub webhook endpoint. Verifies the signature, answers at once, and works in the background.
import postgres from "npm:postgres@3.4.5";
import { route, scheduled } from "./route.ts";

export { route };

declare const EdgeRuntime: { waitUntil(p: Promise<unknown>): void };

export async function validSignature(secret: string, body: Uint8Array, header: string | null) {
  const m = header?.match(/^sha256=([0-9a-f]{64})$/);
  if (!secret || !m) return false;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["verify"],
  );
  const signature = Uint8Array.from(m[1].match(/../g)!, (h) => parseInt(h, 16));
  return crypto.subtle.verify("HMAC", key, signature, body as BufferSource);
}

// A real database query, so that Supabase counts the project as active.
async function touchDatabase() {
  const sql = postgres(Deno.env.get("SUPABASE_DB_URL") ?? "", { max: 1, prepare: false });
  try {
    await sql`select 1`;
  } finally {
    await sql.end({ timeout: 5 });
  }
}

function background(work: Promise<unknown>) {
  EdgeRuntime.waitUntil(work.catch((e) => console.error(`failed: ${e}`)));
}

if (import.meta.main) {
  Deno.serve(async (req) => {
    if (req.method !== "POST") return new Response("Not found", { status: 404 });
    const body = new Uint8Array(await req.arrayBuffer());
    if (new URL(req.url).pathname.endsWith("/sweep")) {
      const ok = await validSignature(
        Deno.env.get("SWEEP_SECRET") ?? "",
        body,
        req.headers.get("x-sweep-signature"),
      );
      if (!ok) return new Response("Bad signature", { status: 401 });
      await touchDatabase();
      // pg_net sends the task as a JSON string, e.g. "hourly 1795462421".
      const task = new TextDecoder().decode(body).replace(/^"/, "").split(" ")[0];
      background(scheduled(task));
      return new Response("accepted", { status: 202 });
    }
    const ok = await validSignature(
      Deno.env.get("WEBHOOK_SECRET") ?? "",
      body,
      req.headers.get("x-hub-signature-256"),
    );
    if (!ok) return new Response("Bad signature", { status: 401 });
    const event = req.headers.get("x-github-event") ?? "";
    if (event === "ping") return new Response("pong");
    background(route(event, JSON.parse(new TextDecoder().decode(body))));
    return new Response("accepted", { status: 202 });
  });
}
