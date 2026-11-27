// What the bot does for one GitHub event or one scheduled task, whatever delivered it: the
// Supabase webhook endpoint (index.ts) or a GitHub Actions job (actions.ts).
import { answerTerms, openNumbers, refresh, sweep, thanks, welcome } from "./assistant.ts";
import {
  announceFixed,
  announceReleased,
  command,
  deleteMergedBranch,
  labelByFiles,
  reminders,
  stale,
  triage,
} from "./bots.ts";
import { BOT, REPO, termsReply } from "./logic.ts";

const PR_ACTIONS = ["opened", "reopened", "ready_for_review", "synchronize", "edited"];
const FIRST_TIME = ["FIRST_TIME_CONTRIBUTOR", "FIRST_TIMER", "NONE"];

// deno-lint-ignore no-explicit-any
export async function route(event: string, p: any) {
  if (p.repository?.full_name !== REPO) return;
  if (event === "pull_request") {
    const pr = p.pull_request;
    if (p.action === "closed") {
      if (pr.merged) {
        await thanks(pr.number, pr.user.login, pr.user.type);
        await announceFixed(pr);
        await deleteMergedBranch(pr).catch((e) => console.error(`branch pr=${pr.number}: ${e}`));
      }
    } else if (PR_ACTIONS.includes(p.action)) {
      if (p.action === "opened" && FIRST_TIME.includes(pr.author_association)) {
        await welcome(pr.number, pr.user.login);
      }
      if (["opened", "reopened", "synchronize"].includes(p.action)) {
        await labelByFiles(pr.number).catch((e) => console.error(`labels pr=${pr.number}: ${e}`));
      }
      await refresh(pr.number);
    }
  } else if (event === "issues") {
    if (["opened", "edited"].includes(p.action)) await triage(p.issue);
  } else if (event === "release") {
    if (p.action === "published") await announceReleased(p.release);
  } else if (event === "pull_request_review") {
    await refresh(p.pull_request.number);
  } else if (event === "issue_comment") {
    if (!["created", "edited"].includes(p.action)) return;
    if ([BOT, "github-actions[bot]"].includes(p.comment.user.login)) return;
    if (p.action === "created") {
      await command(p).catch((e) => console.error(`command: ${e}`));
    }
    if (!p.issue.pull_request) return;
    if (termsReply(p.comment.body) === null) return; // not addressed to the bot: no API calls
    await answerTerms(p.issue.number, p.comment, p.issue.user.id);
    await refresh(p.issue.number);
  } else if (event === "workflow_run" && p.action === "completed") {
    const run = p.workflow_run;
    for (const n of await openNumbers(run.head_sha)) {
      await refresh(n, run.check_suite_id, run.conclusion ?? "");
    }
  } else if (event === "push" && p.ref === "refs/heads/main") {
    await sweep(); // a merge can leave other pull requests in conflict
  }
}

/** "sweep" (open pull requests), "hourly" (also reminders) or "daily" (also stale ones). */
export async function scheduled(task: string) {
  await sweep();
  if (task === "hourly") await reminders();
  if (task === "daily") await stale();
}
