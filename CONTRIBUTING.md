# Contributing

Thank you for helping! Bug reports, ideas and pull requests are welcome.

## Before you open a pull request

You need **Java 17**.

```bash
./gradlew build
```

The jar is written to `build/libs/`. To try it, put it in a Fabric 1.20.1 instance together with
Fabric API and Simple Voice Chat. CI builds on macOS, Windows and Linux.

- Textures and the device preview are generated: edit `tools/make_textures.py` or
  `tools/make_preview.py` and run them, rather than editing the PNG files by hand.
- Keep every player-facing string in `assets/venuesound/lang/` (English and Simplified Chinese).

### The bot

A bot account (`@TonyD365-Agent`) helps on pull requests and issues. **It only answers comments
that start with a mention of it**, for example `@TonyD365-Agent /retest`.

- `/retest` re-runs the checks that failed or were cancelled; `/ready` marks a draft ready for
  review; `/close` and `/reopen`; `/update` merges `main` into your branch (you or a
  maintainer; on a fork, a maintainer).
- After merging, the bot deletes the pull request's branch (not a fork's). `/keep-branch`
  keeps it and `/delete-branch` undoes that (you or a maintainer).
- `/remind 3d` (or `12h`, up to `30d`, optionally with a note) has the bot remind you there then.
- `/label` and `/unlabel` add or remove labels (maintainers: the owner and collaborators).
- `/help` lists them. In an issue, `/close`, `/reopen`, `/label`, `/unlabel` and `/help` work
  too.

It keeps one status comment on each pull request up to date: which checks passed or failed,
what a failure usually means, and what happens next. It sums the checks up in one status,
**VenueSound checks**, which the ruleset requires; once the maintainer approves, it merges the
pull request as soon as that status is green. It also keeps **status labels** up to date
(`Checks-Running`, `Checks-Passed`, `Checks-Failed`, `Internal-Error`, `Merge-Conflict`,
`Needs-Author-Action`, `Changes-Requested`, `Needs-Terms-Agreement`, `Needs-Review`,
`Maintainer-Approved`), labels pull requests by the files they change, and labels new issues.

A pull request that waits on its author (`Needs-Author-Action`) with no push or comment from
them for 30 days gets the `Stale` label and a reminder, and is closed after 30 more days. It
can always be reopened.

## Contribution terms

On your first pull request the bot posts these terms as a comment. Reply with a comment that
says exactly

    @TonyD365-Agent I agree to the VenueSound contribution terms

and the check called **Contribution terms** turns green (it stays red until then). By replying
you agree that:

1. **License.** Your contribution is released under the [MIT License](LICENSE), like the rest
   of the project.
2. **It is yours to give.** You wrote it, or you have the right to contribute it under that
   license. Code from elsewhere is marked, with its source and license, and that license allows
   this.
3. **It may be changed or declined.** The maintainer may edit, rearrange or decline a
   contribution, and you are credited in the project's history (git), not paid.
4. **No warranty.** You give it as is, like the rest of the project.
5. **Conduct.** You follow the [code of conduct](CODE_OF_CONDUCT.md).

The maintainers and bots are not asked. Your answer is recorded in an issue the bot keeps, so
your later pull requests count as agreed too, until you withdraw by replying
`@TonyD365-Agent I do not agree to the VenueSound contribution terms`.
