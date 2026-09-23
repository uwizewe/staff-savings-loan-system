export function selectReport(type, data, { memberId = "", from = "", to = "" } = {}) {
  const inRange = (value) => (!from && !to) || Boolean(value && (!from || value >= from) && (!to || value <= to));
  const memberMatches = (row) => !memberId || String(type === "members" ? row.id : row.memberId) === String(memberId);
  let rows = [];
  if (type === "members") rows = data.members.filter((r) => memberMatches(r) && inRange(r.joiningDate));
  if (["savings", "monthly-savings"].includes(type)) rows = data.savings.filter((r) => memberMatches(r) && inRange(r.transactionDate) && (type !== "monthly-savings" || r.savingType === "MONTHLY"));
  if (["loans", "active-loans", "outstanding", "left-outstanding"].includes(type)) {
    const leftIds = new Set(data.members.filter((r) => r.membershipStatus === "LEFT").map((r) => String(r.id)));
    rows = data.loans.filter((r) => memberMatches(r) && inRange(r.applicationDate)
      && (type !== "active-loans" || r.loanStatus === "ACTIVE")
      && (!["outstanding", "left-outstanding"].includes(type) || (r.loanStatus === "ACTIVE" && Number(r.outstandingBalance) > 0))
      && (type !== "left-outstanding" || leftIds.has(String(r.memberId))));
  }
  if (type === "repayments") rows = data.repayments.filter((r) => memberMatches(r) && inRange(r.paymentDate));
  if (["income", "expenses", "income-expense"].includes(type)) rows = data.finance.filter((r) => inRange(r.transactionDate) && (type === "income-expense" || r.financeType === (type === "income" ? "INCOME" : "EXPENSE")));
  if (from && to && from > to) rows = [];
  const approved = rows.filter((r) => r.status === "APPROVED");
  const sum = (items, key = "amount", sign = () => 1) => items.reduce((total, r) => total + Math.round(Number(r[key] || 0) * 100) * sign(r), 0) / 100;
  const total = (label, value) => ({ label, value, money: true });
  let totals = [{ label: "Records", value: rows.length }];
  if (["savings", "monthly-savings"].includes(type)) totals = [total("Approved net savings", sum(approved, "amount", (r) => r.savingType === "WITHDRAWAL" ? -1 : 1))];
  if (["repayments", "income", "expenses"].includes(type)) totals = [total("Approved total", sum(approved))];
  if (type === "income-expense") {
    const income = sum(approved.filter((r) => r.financeType === "INCOME"));
    const expenses = sum(approved.filter((r) => r.financeType === "EXPENSE"));
    totals = [total("Approved income", income), total("Approved expenses", expenses), total("Net income", Math.round((income - expenses) * 100) / 100)];
  }
  if (type === "loans") totals = [total("Requested", sum(rows, "requestedAmount")), total("Active outstanding", sum(rows.filter((r) => r.loanStatus === "ACTIVE"), "outstandingBalance"))];
  if (["active-loans", "outstanding", "left-outstanding"].includes(type)) totals = [total("Outstanding", sum(rows, "outstandingBalance"))];
  return { rows, totals };
}
