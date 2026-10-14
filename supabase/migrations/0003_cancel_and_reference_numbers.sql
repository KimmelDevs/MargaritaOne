-- Run after 0002. Adds report reference numbers and lets citizens cancel their own pending reports.

-- New status. (ALTER TYPE ... ADD VALUE must run outside other statements that use the value,
-- so nothing below compares against 'cancelled' at definition time.)
alter type public.report_status add value if not exists 'cancelled';

-- Human-friendly, ever-increasing number shown as e.g. MG-2026-0042 in the app.
alter table public.reports
    add column if not exists ref_no bigint generated always as identity;

create unique index if not exists reports_ref_no_idx on public.reports (ref_no);

-- Citizens may withdraw their own report while it is still pending.
-- Goes through the timeline like every other status change (trigger copies the status onto reports).
create or replace function public.cancel_my_report(target_report uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if not exists (
        select 1 from public.reports
        where id = target_report
          and reporter_id = auth.uid()
          and status = 'pending'
    ) then
        raise exception 'Only your own pending reports can be cancelled' using errcode = '42501';
    end if;

    insert into public.report_updates (report_id, author_id, status, note)
    values (target_report, auth.uid(), 'cancelled'::public.report_status, 'Cancelled by the reporter');
end;
$$;

revoke execute on function public.cancel_my_report(uuid) from public, anon;
grant execute on function public.cancel_my_report(uuid) to authenticated;
