import { ArrowLeft, Download, Printer } from "lucide-react";
import { useAuth } from "../../auth/hooks/useAuth.jsx";
import { Button, Card, DataTable, ErrorState, Loading, MoneyCell, PageHeader, StatusBadge } from "../../../components/ui/index.jsx";
import { useApiData } from "../../../hooks/useApiData.js";
import { date, downloadCsv, money, routeTo } from "../../../utils/index.js";

export default function StatementPage({ route }) {
  const { user } = useAuth();
  const requestedId = new URLSearchParams(route.split("?")[1] || "").get("id");
  const memberId = requestedId || user.memberId;
  const { data, loading, error, reload } = useApiData(memberId ? `/members/${memberId}/statement` : "/dashboard", [memberId]);
  if (!memberId) return <Card>No member is linked to this account.</Card>;
  if (loading) return <Loading label="Preparing member statement…" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;

  const exportStatement = () => {
    const rows = [
      ...data.repayments.map((item) => ({ date: item.paymentDate, type: "LOAN REPAYMENT", reference: item.reference, debit: item.amount, credit: "", status: item.status })),
    ].sort((a, b) => String(a.date).localeCompare(String(b.date)));
    downloadCsv(`${data.member.memberCode}-statement.csv`, rows);
  };

  return (
    <>
      <PageHeader title="Loan Statement" description={`${data.member.memberCode} · ${data.member.fullName}`} actions={<><Button variant="secondary" onClick={() => routeTo(user.role === "MEMBER" ? "dashboard" : "members")}><ArrowLeft size={15} />Back</Button><Button variant="secondary" onClick={() => window.print()}><Printer size={15} />Print</Button><Button onClick={exportStatement}><Download size={15} />Export CSV</Button></>} />
      <Card className="mb-6 bg-gradient-to-r from-[#073d35] to-[#0f766e] text-white">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4"><div><p className="text-xs text-teal-100/70">Member</p><p className="mt-1 font-bold">{data.member.fullName}</p><p className="text-xs text-teal-100/60">{data.member.department}</p></div><div><p className="text-xs text-teal-100/70">Loan repayments</p><p className="mt-1 text-xl font-bold">{money(data.totalRepaid)}</p></div><div><p className="text-xs text-teal-100/70">Outstanding loans</p><p className="mt-1 text-xl font-bold">{money(data.outstandingLoans)}</p></div></div>
      </Card>
      <section className="mb-7"><h2 className="mb-3 font-bold text-slate-900">Loans</h2><DataTable rows={data.loans} columns={[
        { key: "applicationNumber", label: "Loan number", className: "font-semibold" }, { key: "applicationDate", label: "Application", render: (row) => date(row.applicationDate) }, { key: "approvedAmount", label: "Approved", render: (row) => <MoneyCell value={row.approvedAmount} /> }, { key: "totalPayable", label: "Total payable", render: (row) => <MoneyCell value={row.totalPayable} /> }, { key: "outstandingBalance", label: "Outstanding", render: (row) => <MoneyCell value={row.outstandingBalance} /> }, { key: "loanStatus", label: "Status", render: (row) => <StatusBadge value={row.loanStatus} /> },
      ]} /></section>
      <section><h2 className="mb-3 font-bold text-slate-900">Loan repayments</h2><DataTable rows={data.repayments} columns={[
        { key: "paymentDate", label: "Date", render: (row) => date(row.paymentDate) }, { key: "loanNumber", label: "Loan" }, { key: "reference", label: "Reference", className: "font-semibold" }, { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> }, { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> },
      ]} /></section>
    </>
  );
}

