import test from "node:test";
import assert from "node:assert/strict";
import { selectReport } from "./reportData.js";

const empty = { members: [], savings: [], loans: [], repayments: [], finance: [] };
test("income vs expenses separates approved totals and calculates net", () => {
  const report = selectReport("income-expense", { ...empty, finance: [
    { financeType: "INCOME", amount: 100, status: "APPROVED" },
    { financeType: "EXPENSE", amount: 30, status: "APPROVED" },
    { financeType: "INCOME", amount: 500, status: "DRAFT" },
  ] });
  assert.deepEqual(report.totals.map((r) => r.value), [100, 30, 70]);
});
test("savings subtracts approved withdrawals and preserves cents", () => {
  const report = selectReport("savings", { ...empty, savings: [
    { savingType: "MONTHLY", amount: 100.10, status: "APPROVED" },
    { savingType: "WITHDRAWAL", amount: 20.05, status: "APPROVED" },
    { savingType: "MONTHLY", amount: 500, status: "REJECTED" },
  ] });
  assert.equal(report.totals[0].value, 80.05);
});
test("outstanding excludes approved but undisbursed loans", () => {
  const report = selectReport("outstanding", { ...empty, loans: [
    { id: 1, loanStatus: "APPROVED", outstandingBalance: 100 },
    { id: 2, loanStatus: "ACTIVE", outstandingBalance: 50 },
  ] });
  assert.deepEqual(report.rows.map((r) => r.id), [2]);
});
test("date and member filters are inclusive and do not use transaction ids", () => {
  const data = { ...empty, repayments: [
    { id: 9, memberId: 1, paymentDate: "2026-09-01" },
    { id: 1, memberId: 2, paymentDate: "2026-09-30" },
    { id: 3, memberId: 1, paymentDate: "2026-10-01" },
  ] };
  assert.deepEqual(selectReport("repayments", data, { memberId: "1", from: "2026-09-01", to: "2026-09-30" }).rows.map((r) => r.id), [9]);
  assert.equal(selectReport("repayments", data, { from: "2026-10-01", to: "2026-09-01" }).rows.length, 0);
});
test("member report dates filter joining dates", () => {
  const data = { ...empty, members: [{ id: 1, joiningDate: "2026-01-01" }, { id: 2, joiningDate: "2025-01-01" }] };
  assert.deepEqual(selectReport("members", data, { from: "2026-01-01" }).rows.map((r) => r.id), [1]);
});
