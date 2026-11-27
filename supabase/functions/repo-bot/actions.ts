// The Actions version: a GitHub Actions job runs this for one event, then exits. Same code as
// the Supabase version (route.ts); only the delivery differs. See actions-bot/README.md.
import { route, scheduled } from "./route.ts";

const SCHEDULES: Record<string, string> = { "41 * * * *": "hourly", "23 8 * * *": "daily" };

if (!Deno.env.get("GITHUB_TOKEN")) {
  // Events from forks (such as a review on a fork's pull request) get no secrets; the next
  // scheduled sweep picks them up.
  console.log("No bot token for this event; the scheduled sweep will catch up.");
} else {
  const event = Deno.env.get("GITHUB_EVENT_NAME") ?? "";
  const payload = JSON.parse(await Deno.readTextFile(Deno.env.get("GITHUB_EVENT_PATH") ?? ""));
  if (event === "schedule") {
    await scheduled(SCHEDULES[payload.schedule] ?? "sweep");
  } else if (event === "workflow_dispatch") {
    await scheduled(String(payload.inputs?.task ?? "sweep"));
  } else {
    // pull_request_target carries the same payload as pull_request, with the bot's secrets.
    await route(event === "pull_request_target" ? "pull_request" : event, payload);
  }
}
