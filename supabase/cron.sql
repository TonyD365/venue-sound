-- Schedules for the bot: a sweep every 10 minutes (catches missed webhooks), reminders every
-- hour, stale pull requests every day. Two Vault secrets are read here:
--   sweep_secret  the same value as the function's SWEEP_SECRET
--   function_url  https://<project-ref>.supabase.co/functions/v1/repo-bot
-- Create them first (README, step 5), then run this file once.
create extension if not exists pg_cron;
create extension if not exists pg_net;
create extension if not exists pgcrypto with schema extensions;
create schema if not exists repo_bot;
revoke all on schema repo_bot from public;

create or replace function repo_bot.call(task text) returns bigint
language plpgsql security definer set search_path = '' as $$
declare
  payload jsonb := to_jsonb(task || ' ' || extract(epoch from now())::bigint);
  key text;
  url text;
begin
  select decrypted_secret into strict key from vault.decrypted_secrets where name = 'sweep_secret';
  select decrypted_secret into strict url from vault.decrypted_secrets where name = 'function_url';
  return net.http_post(
    url := url || '/sweep',
    body := payload,
    headers := jsonb_build_object(
      'content-type', 'application/json',
      'x-sweep-signature', 'sha256=' || encode(
        extensions.hmac(convert_to(payload::text, 'utf8'), convert_to(key, 'utf8'), 'sha256'), 'hex')
    )
  );
end $$;
revoke all on function repo_bot.call(text) from public;

select cron.schedule('repo-bot-sweep', '*/10 * * * *', $$select repo_bot.call('sweep')$$);
select cron.schedule('repo-bot-hourly', '41 * * * *', $$select repo_bot.call('hourly')$$);
select cron.schedule('repo-bot-daily', '23 8 * * *', $$select repo_bot.call('daily')$$);
