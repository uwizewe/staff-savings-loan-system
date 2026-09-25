# Loan workflow

## Categories and applications

Administrators manage names, annual percentage rates, descriptions and active status under **System Settings → Loan Categories**. Existing loans retain their agreed category/rate snapshot when a category changes. Inactive categories cannot be selected for new applications.

In **Loans → Loan applications**, choose staff, category, principal, installment count and first installment date. Select **View full breakdown** before saving or submitting. The backend calculates reducing-balance installments using the annual rate divided by 12. Interest applies to each opening principal balance; the last installment absorbs rounding. Due dates follow the selected first installment date, including month-end handling. The saved application retains its schedule.

For example, 1,000 at 12% annually over 12 installments has a regular installment of 88.85 and total interest of 66.19. Its first installment contains 10.00 interest and 78.85 principal.

Submission and approval enforce one committed loan per staff member. A database reservation prevents concurrent pending, approved or active loans. Independent approval immediately sets the loan to ACTIVE and activates its installment schedule. There is no separate disbursement action. Existing independently approved loans are activated by the idempotent startup upgrade, with an audit entry.

## Active loan dashboard and actions

Open **Loans → Active Loan → View dashboard**. This is a dedicated page. Select **View Installment Schedule** to open the scrollable schedule and history modal. The dashboard shows staff details, loan terms, principal/interest/paid/balance summaries, installment progress, the current schedule, schedule versions and repayment history. Actions open separate dialogs on the dashboard page. Closed Loan lists completed/closed loans and retains access to their details, schedules and repayments.

- **Top Up:** add money to remaining principal and preview the proposed term and schedule.
- **Reschedule:** change the remaining term without adding principal.
- **Restructure:** add money and change the remaining term together.
- **Installment Payment:** select an unpaid installment and record its payment date, amount and description. Principal and interest allocations are calculated by the backend.
- **Full Settlement:** remaining principal plus one monthly term's interest on that principal, capped by remaining contractual interest, plus any configured settlement charge. Approval waives the other future interest and closes the loan.

Changes are proposals until independently approved. Approval activates the new version; rejection retains the proposal as rejected history. Old schedules are retained read-only. Payments always apply to the current active version. Top-up funds enter the loan balance on approval; these actions do not create a separate finance-ledger disbursement.

Resolve pending repayments, overdue installments and partially paid interest before requesting a schedule change. Payment approvals are blocked while a change is pending. Effective dates cannot precede approved payments or exceed today.

## Monthly repayment approval

Select the month/year in **Loans → Monthly Loan**, enter the batch description, select due installments and enter payment amounts. Staff IDs/names, loan references, installment numbers, principal, interest and balances are available for review.

Approvers see the batch description, staff/payment rows and aggregate totals before deciding. Approval revalidates every payment and applies the entire batch in one transaction. If any installment has changed or been paid, the batch rolls back. Fully paid installments cannot receive duplicate payments. The initiator cannot approve their own request.

Standalone installment payments and full settlements also require independent approval. Approved allocations and waived interest remain in the payment history. Ordinary complete repayment releases the staff loan reservation; full settlement sets the loan to CLOSED.

## Existing records and upgrade

The upgrade preserves existing balances, dates and payments and records existing schedules as a baseline version. Earlier schedules discarded before this upgrade cannot be reconstructed. Validated legacy flat-rate schedules receive their original equal-principal breakdown only when their row count and totals match the loan. Inconsistent legacy records require explicit reconciliation before schedule changes or settlement.

Repayments prepared before schedule versioning may need rejection and recreation if approval reports that the schedule changed. This prevents approval against an unverified allocation.

New member registration IDs use `VFC%03d`: VFC001, VFC002, …, VFC999, VFC1000. A database-locked persistent counter advances above the highest existing numeric VFC ID, including legacy VFC-001 formatting. Existing IDs are preserved. Registration and counter allocation share one transaction, and the member-code unique constraint remains enforced.

## Verification and runtime

The application uses MySQL via `backend/src/main/resources/application.properties`; the API runs on port 8092 and the frontend on 8090. Backend integration tests use isolated H2 databases, never the live MySQL database.

Validated: 25 backend tests, 5 frontend report tests, production frontend build and project structure checks. Coverage includes schedule version retention, reducing-balance calculations, category deactivation, first installment dates, one-term settlement, duplicate-payment prevention and monthly-batch rollback.

A MySQL backup was created before applying this upgrade in `backend/backups/`. The updated backend has been restarted. Refresh the frontend to load the current UI.

## Savings UI and statements

Savings navigation contains Members Saving, Monthly Saving and Savings History, with no tab strip. Members Saving lists member IDs/names, monthly contributions, total balances and status. View Savings History opens the dedicated Savings Dashboard. Its Savings Statement contains only savings transactions and shows date, transaction, required comment, signed amount, running approved balance and status. Monthly comments use a label such as September 2026 Monthly Saving.

Member Savings Transaction opens the Deposit/Withdraw dialog. Transactions retain independent approval: only approved deposits increase balances, and approved withdrawals reduce them. The API requires a comment and checks available savings on creation and again under a member lock at withdrawal approval.

Loan Statement is separate and contains only loans and repayments. The Member List Action dropdown opens Savings Dashboard, Active Loan Dashboard or Closed Loans.

All shared data tables support search, sortable data columns, pagination, page size, empty/loading states and horizontal scrolling. Verified savings and loan dashboards at mobile (390px), tablet (768px) and desktop widths; schedule pagination and filtering were checked without submitting live financial transactions.

