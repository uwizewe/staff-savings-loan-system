# Reports module

The Reports sidebar contains 16 separate pages, grouped into general, savings, loan, finance and administration reports. Each page generates from a read-only backend snapshot, then uses the same filtered data for its screen, PDF, Excel and print outputs. Exports contain all matching rows, independently of on-screen pagination. Table sorting is a viewing preference; exports retain statement order.

## Use

Open http://localhost:8090/#reports?page=vsa-account and choose a report in the sidebar. Select dates and any member/type/status filters, then press Generate. Filter changes disable export until Generate is pressed. Member Statement requires one member; it displays separate Savings Statement and Loan Statement tables. Savings Report links to the selected member's Savings Dashboard.

Print opens a paginated PDF in a new browser tab. Allow the local application's pop-up if the browser blocks it. PDF and Excel files include organization, report name, reporting period, generated date, filters, data and summaries. PDF pages and Excel printed sheets include page numbers.

## Accounting conventions

- Approved loan activation and approved additional funding are cash outflows. Rescheduling without additional funding is not a cash movement.
- Approved repayment total is a single cash inflow. All Transactions splits that amount into principal, interest, penalty (when recorded), and other charges; it does not add a duplicate repayment-total row.
- Loan principal and savings are excluded from income. Loan interest, recorded penalties and existing settlement charges are income. The existing repayment model stores settlement charges rather than a distinct penalty field; charges are displayed separately and are never relabelled penalties.
- Finance entries are independent posted transactions. A supporting reference alone is not a formal link that reverses or deduplicates another transaction.
- Withdrawals reduce savings. Pending/rejected transactions do not affect balances or approved totals.
- Account opening balance is calculated from recorded transactions before From Date, starting at zero. An unrecorded opening cash balance is not invented.
- Savings balances include approved transactions through To Date. Deposit/withdrawal totals cover the selected period.
- Loan balances use schedule versions and repayment allocations through To Date. Approved schedule replacements supersede the old remaining schedule. Full-settlement waived interest reduces the loan balance without becoming cash or income.
- Loan report dates filter application dates; Closed Loans uses the final approved repayment date; arrears uses installment due dates. Due is the cutoff day, Overdue is 1–30 days late, Seriously Overdue is over 30 days late.
- Historical monthly contribution rates and discarded pre-upgrade schedules were not stored. Monthly expected savings uses the current configured contribution. Legacy repayment allocation gaps are identified in report notes rather than assigned invented principal/interest amounts.
- Audit Report is restricted to ADMIN. Association reports are available to ADMIN, INITIATOR and APPROVER, not MEMBER.

## Verification

Frontend: `cd frontend` then `npm test`, `npm run build`, `npm run check:structure`.
Backend: run the Maven test suite. Report endpoint coverage is in LoanEndpointTest and uses H2, not the live MySQL database.

Report calculation tests cover funding, top-ups, same-day schedule replacements, full settlement, arrears, cash reconciliation, savings-only balances, income classification, filters, and multi-page PDF/Excel output.
