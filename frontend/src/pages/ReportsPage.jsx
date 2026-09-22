import { useState } from "react";
import { Download, FileBarChart, Printer } from "lucide-react";
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, MoneyCell, PageHeader, StatusBadge } from "../components/ui";
import { useApiData } from "../hooks";
import { date, downloadCsv, money } from "../utils";

import { selectReport } from "../reportData";

const reportTypes = [
  ["members", "Member list"], ["savings", "Member savings"], ["monthly-savings", "Monthly savings"],
  ["loans", "Loan applications"], ["active-loans", "Active loans"], ["outstanding", "Outstanding loans"],
  ["left-outstanding", "Members left with outstanding loans"], ["repayments", "Loan repayments"],
  ["income", "Income"], ["expenses", "Expenses"], ["income-expense", "Income vs expense"],
];

export default function ReportsPage() {
  const members = useApiData("/members");
  const savings = useApiData("/savings");
  const loans = useApiData("/loans");
  const repayments = useApiData("/loans/repayments");
  const finance = useApiData("/finance");
  const [type, setType] = useState("members");
  const [memberId, setMemberId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  const loading = members.loading || savings.loading || loans.loading || repayments.loading || finance.loading;
  const error = members.error || savings.error || loans.error || repayments.error || finance.error;
  if (loading) return <Loading label="Preparing reports…" />;
  if (error) return <ErrorState message={error} onRetry={() => { members.reload(); savings.reload(); loans.reload(); repayments.reload(); finance.reload(); }} />;

  const report = buildReport(type, { members: members.data, savings: savings.data, loans: loans.data, repayments: repayments.data, finance: finance.data }, { memberId, from, to });
  const invalidRange = Boolean(from && to && from > to);
  const title = reportTypes.find(([value]) => value === type)?.[1];
  const exportRows = report.rows.map((row) => Object.fromEntries(report.columns.filter((column) => column.key !== "actions").map((column) => [column.label, row[column.key] ?? ""])));

  return (
    <>
      <PageHeader title="Reports" description="Filter, review, print and export operational financial records." actions={<div className="no-print flex gap-2"><Button variant="secondary" disabled={invalidRange || !report.rows.length} onClick={() => window.print()}><Printer size={15} />Print</Button><Button onClick={() => downloadCsv(`${type}-${new Date().toISOString().slice(0, 10)}.csv`, exportRows)} disabled={!report.rows.length}><Download size={15} />Export CSV</Button></div>} />
      <Card className="mb-6 no-print">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4"><Field label="Report"><select className={inputClass} value={type} onChange={(e) => setType(e.target.value)}>{reportTypes.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field><Field label="Member"><select disabled={["income", "expenses", "income-expense"].includes(type)} className={inputClass} value={memberId} onChange={(e) => setMemberId(e.target.value)}><option value="">All members</option>{members.data.map((member) => <option key={member.id} value={member.id}>{member.memberCode} — {member.fullName}</option>)}</select></Field><Field label="From date"><input className={inputClass} type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field><Field label="To date"><input className={inputClass} type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field></div>
      </Card>
      <div className="report-summary mb-4 flex flex-col gap-3 rounded-2xl bg-gradient-to-r from-[#073d35] to-[#0f766e] p-5 text-white sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3"><span className="grid size-10 place-items-center rounded-xl bg-white/10"><FileBarChart size={21} /></span><div><p className="font-bold">{title}</p><p className="text-xs text-teal-100/70">{report.rows.length} matching record{report.rows.length === 1 ? "" : "s"}</p></div></div><div className="flex gap-6">{report.totals?.map((item) => <div key={item.label} className="text-right"><p className="text-[10px] uppercase tracking-wider text-teal-100/60">{item.label}</p><p className="font-bold">{item.money ? money(item.value) : item.value}</p></div>)}</div></div>
      {invalidRange && <p role="alert" className="mb-4 text-sm text-rose-700">From date must be on or before To date.</p>}
      <p className="mb-3 text-xs text-slate-500">{from || "Any date"} to {to || "Any date"} · {["income", "expenses", "income-expense"].includes(type) || !memberId ? "All members" : members.data.find((m) => String(m.id) === memberId)?.fullName}{type === "members" ? " · Dates filter joining date; balances are current." : ["loans", "active-loans", "outstanding", "left-outstanding"].includes(type) ? " · Dates filter application date; balances are current." : " · Financial totals include approved records only."}</p>
      <div className="no-print"><DataTable key={`${type}-${memberId}-${from}-${to}`} rows={report.rows} columns={report.columns} pageSize={15} /></div>
      <div className="report-print-only"><DataTable rows={report.rows} columns={report.columns} pageSize={Math.max(1, report.rows.length)} /></div>
    </>
  );
}

function buildReport(type, data, filters) {
  const memberCols = [{ key: "memberCode", label: "Member ID" }, { key: "fullName", label: "Full name" }, { key: "department", label: "Department" }, { key: "phone", label: "Phone" }, { key: "membershipStatus", label: "Membership", render: (r) => <StatusBadge value={r.membershipStatus} /> }, { key: "riskStatus", label: "Risk", render: (r) => <StatusBadge value={r.riskStatus} /> }, { key: "totalSavings", label: "Savings", render: (r) => <MoneyCell value={r.totalSavings} /> }, { key: "outstandingLoans", label: "Outstanding", render: (r) => <MoneyCell value={r.outstandingLoans} /> }];
  const savingCols = [{ key: "transactionDate", label: "Date", render: (r) => date(r.transactionDate) }, { key: "reference", label: "Reference" }, { key: "memberName", label: "Member" }, { key: "savingType", label: "Type" }, { key: "amount", label: "Amount", render: (r) => <MoneyCell value={r.amount} /> }, { key: "status", label: "Status", render: (r) => <StatusBadge value={r.status} /> }];
  const loanCols = [{ key: "applicationNumber", label: "Loan number" }, { key: "memberName", label: "Member" }, { key: "applicationDate", label: "Application date", render: (r) => date(r.applicationDate) }, { key: "requestedAmount", label: "Requested", render: (r) => <MoneyCell value={r.requestedAmount} /> }, { key: "approvedAmount", label: "Approved", render: (r) => <MoneyCell value={r.approvedAmount} /> }, { key: "outstandingBalance", label: "Outstanding", render: (r) => <MoneyCell value={r.outstandingBalance} /> }, { key: "loanStatus", label: "Status", render: (r) => <StatusBadge value={r.loanStatus} /> }];
  const repaymentCols = [{ key: "paymentDate", label: "Date", render: (r) => date(r.paymentDate) }, { key: "reference", label: "Reference" }, { key: "memberName", label: "Member" }, { key: "loanNumber", label: "Loan" }, { key: "amount", label: "Amount", render: (r) => <MoneyCell value={r.amount} /> }, { key: "status", label: "Status", render: (r) => <StatusBadge value={r.status} /> }];
  const financeCols = [{ key: "transactionDate", label: "Date", render: (r) => date(r.transactionDate) }, { key: "reference", label: "Reference" }, { key: "financeType", label: "Type" }, { key: "category", label: "Category" }, { key: "description", label: "Description" }, { key: "amount", label: "Amount", render: (r) => <MoneyCell value={r.amount} /> }, { key: "status", label: "Status", render: (r) => <StatusBadge value={r.status} /> }];
  const { rows, totals } = selectReport(type, data, filters);
  const columns = type === "members" ? memberCols
    : ["savings", "monthly-savings"].includes(type) ? savingCols
    : ["loans", "active-loans", "outstanding", "left-outstanding"].includes(type) ? loanCols
    : type === "repayments" ? repaymentCols : financeCols;
  return { rows, columns, totals };
}