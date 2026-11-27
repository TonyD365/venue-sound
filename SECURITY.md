# Security

## Reporting a problem

**Please do not open a public issue for a security problem.** Report it privately: open the
repository's **Security** tab and choose **Report a vulnerability**.

Say what the problem is, how to reproduce it and which version you used. You will get an
answer within a week. Please give a reasonable time to fix it before telling anyone else.

## What happens next

1. You get an answer saying whether it is a vulnerability and how serious it is.
2. The fix is made privately, and you are welcome to check it.
3. It is released, and a security advisory is published that credits you (unless you would
   rather not be).

## Scope

The latest release and the `main` branch: the mod (for example, a client able to change
another player's devices or crash a server through a packet) and the bot in `supabase/functions/`:
a way to make it write as the bot account, merge without approval, or accept a forged webhook
is in scope.
