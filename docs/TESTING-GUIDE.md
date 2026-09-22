# Guided Acceptance Test

Run the backend and frontend using the root README, then complete this workflow.

## 1. Member registration

1. Sign in as `initiator`.
2. Open **Members → Add member**.
3. Register an active member with a unique Member ID.
4. Confirm the member appears in the table.

Expected: member details, membership status and risk status are shown independently.

## 2. Individual savings approval

1. As `initiator`, open **Savings → Individual savings**.
2. Create and submit a contribution.
3. Sign out and sign in as `approver`.
4. Open **Approvals** and approve it.
5. Return to savings history.

Expected: status is Approved and the member savings balance increases.

## 3. Loan lifecycle

1. As `initiator`, create and submit a loan application.
2. As `approver`, approve the loan from **Approvals**.
3. As `initiator`, open the loan and choose **Disburse**.
4. Enter the date and payment reference.
5. Open the repayment schedule.

Expected: the loan becomes Active, flat interest is calculated, and all schedule rows add exactly to the total payable.

## 4. Loan repayment

1. As `initiator`, record and submit a repayment.
2. As `approver`, approve it.
3. Review the loan and its schedule.

Expected: the outstanding balance decreases and the earliest unpaid installment is paid first. A repayment larger than the outstanding balance must be rejected.

## 5. Member leaving with an active loan

1. Edit a member who has an active loan.
2. Set membership to **Left**, enter an exit date, and optionally set risk to **Watchlist**.
3. Open the member statement and outstanding-loans report.

Expected: the member remains visible, the active loan is unchanged, and repayments can continue. A new loan application for that member is blocked by default.

## 6. Income and expenses

1. As `initiator`, record and submit one income and one expense.
2. As `approver`, approve both.
3. Open **Reports → Income vs expense**.

Expected: both approved transactions are present and can be exported to CSV.

## 7. Separation of duties and audit

1. As `admin`, create a draft transaction and submit it.
2. Try to approve the same transaction with the same account.
3. Sign in as a different Approver and approve it.
4. Review **Administration → Audit logs**.

Expected: self-approval is blocked. The audit log shows creator, submitter, approver, timestamps and status movement.

## Automated checks

Frontend production build:

```bash
cd frontend
npm install
npm run build
```

Backend tests:

```bash
cd backend
./mvnw test
```

On Windows, use `.\mvnw.cmd test`.

