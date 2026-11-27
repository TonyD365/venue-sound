// Everything specific to one repository. The names come from Supabase secrets (see the
// README); the rules below are yours to edit.

const env = (name: string, fallback: string) => Deno.env.get(name) || fallback;

/** "owner/name" of the repository the bot works for. */
export const REPO = env("REPO", "owner/project");
/** The bot's GitHub login: it writes as this account, and comments must mention it. */
export const BOT = env("BOT_LOGIN", "example-bot");
/** Shown in comments and in the summary status "<project> checks". */
export const PROJECT = env("PROJECT_NAME", REPO.split("/")[1]);
/** Logins that never need to agree to the contribution terms (comma-separated). */
export const MAINTAINERS = env("MAINTAINERS", REPO.split("/")[0])
  .split(",").map((s) => s.trim()).filter(Boolean);
/** The one status a ruleset should require. */
export const CHECK_CONTEXT = `${PROJECT} checks`;

/** What a contributor runs before pushing (shown in the welcome comment). */
export const LOCAL_CHECKS = [
  "./gradlew build",
];

/** What a failed check usually means, by check name. The first match wins. */
export const HINTS: [RegExp, string][] = [
  [
    /^Build/,
    "The mod did not compile: open the log, then run `./gradlew build` locally (Java 17).",
  ],
  [
    /Bot function tests/,
    "Run `deno fmt`, `deno check` and `deno test` in supabase/functions/repo-bot.",
  ],
  [/zizmor/, "A workflow file has a security problem: see the annotation on the Files tab."],
  [
    /^CodeQL/,
    "Code scanning found a possible security problem in the changed code: see the annotation on the PR's Files tab.",
  ],
];

/** Issue labels by keywords in the title or body (lowercase). */
export const ISSUE_LABELS: [RegExp, string][] = [
  [/install|crash|mods folder|fabric api|launch/, "installing"],
  [/voice ?chat|svc|microphone|mic\b/, "voice chat"],
  [/mixer|console|fader|cable|speaker|amplifier|outlet|receiver|deck/, "devices"],
  [/windows|win(10|11)/, "platform: Windows"],
  [/macos|mac os|apple silicon|macbook|imac/, "platform: macOS"],
  [/linux|ubuntu|debian|fedora|arch|raspberry/, "platform: Linux"],
];

/** How a bug report names the version (asked for when missing). */
export const VERSION_ASK =
  `the **${PROJECT} version** (the jar name in your mods folder), and your Minecraft and Simple Voice Chat versions`;

/** Pull request labels by changed file. */
export const FILE_LABELS: [string, RegExp][] = [
  ["ci", /^\.github\//],
  ["documentation", /^[^/]+\.md$|^docs\/|^site\/|^scripts\/sync_wiki\.py$/],
  ["workbench", /^workbench\//],
  ["dependencies", /^(build\.gradle|gradle\.properties|settings\.gradle|gradle\/)/],
  ["assets", /^src\/main\/resources\/assets\/|^tools\//],
  ["bot", /^supabase\//],
];
