import { ArrowUpRight, Plus, FileBarChart, Activity, CircleDollarSign, Coins, Landmark, ReceiptText, Users, WalletCards } from "lucide-react";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useAuth } from "../auth";
import { Button, Card, DataTable, ErrorState, Loading, PageHeader, StatCard, StatusBadge } from "../components/ui";
import { useApiData } from "../hooks";
import { dateTime, money, routeTo } from "../utils";

export default function DashboardPage() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useApiData("/dashboard");
  if (loading) return <Loading label="Preparing your dashboard…" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;

  const isMember = user.role === "MEMBER";
  const canApprove = ["ADMIN", "APPROVER"].includes(user.role);
  const cards = isMember ? [
    ["My savings", money(data.totalSavings), Coins, "teal"],
    ["Active loans", data.activeLoans, Landmark, "blue"],
    ["Outstanding balance", money(data.totalOutstandingLoans), CircleDollarSign, "amber"],
    ["Total repaid", money(data.totalLoanRepayments), Activity, "emerald"],
  ] : [
    ["Total members", data.totalMembers, Users, "blue", `${data.activeMembers} active`],
    ["Total savings", money(data.totalSavings), Coins, "teal"],
    ["Active loans", data.activeLoans, Landmark, "violet", `${money(data.totalOutstandingLoans)} outstanding`],
    ["Loan repayments", money(data.totalLoanRepayments), Activity, "emerald"],
    ["Total income", money(data.totalIncome), WalletCards, "blue"],
    ["Total expenses", money(data.totalExpenses), ReceiptText, "rose"],
  ];

  return (
    <>
            <PageHeader title="Financial overview" description={`Welcome back, ${user.fullName.split(" ")[0]}. Here is your workspace at a glance.`} actions={<Button variant="secondary" onClick={() => routeTo(isMember ? "statement" : "reports")}><FileBarChart size={16} />{isMember ? "My statement" : "View reports"}</Button>} />
      <section className="overview-banner mb-6 flex flex-col justify-between gap-5 rounded-2xl border border-teal-100 bg-[#eaf5f1] p-6 sm:flex-row sm:items-center">
        <div><p className="mb-2 text-[10px] font-bold uppercase tracking-[.18em] text-teal-700">Savings & loan management</p><h2 className="text-xl font-semibold tracking-tight text-slate-900">{isMember ? "Your finances, in one place." : "A clear view of your association."}</h2><p className="mt-2 max-w-xl text-sm leading-6 text-slate-600">{isMember ? "Review your savings, follow your loan balance and keep track of repayments." : "Track member savings, manage lending and keep every transaction accounted for."}</p></div>
        <Button onClick={() => routeTo(isMember ? "loans" : canApprove ? "approvals" : "members?view=add")} className="shrink-0 self-start sm:self-center">{isMember ? "View my loans" : canApprove ? "Review approvals" : "Add member"}<ArrowUpRight size={16} /></Button>
      </section>
      <div className={`grid gap-4 sm:grid-cols-2 ${isMember ? "xl:grid-cols-4" : "xl:grid-cols-3"}`}>{cards.map(([label, value, icon, tone, note]) => <StatCard key={label} label={label} value={value} icon={icon} tone={tone} note={note} />)}</div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <Card>
          <div className="mb-5"><h2 className="font-bold text-slate-900">Six-month activity</h2><p className="mt-1 text-xs text-slate-500">Approved savings and loan repayments</p></div>
          <div className="mb-4 flex gap-4 text-xs text-slate-500"><span className="flex items-center gap-2"><i className="size-2 rounded-full bg-teal-700" />Savings</span><span className="flex items-center gap-2"><i className="size-2 rounded-full bg-blue-600" />Repayments</span></div><div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data.monthlyTrend} margin={{ left: 0, right: 10, top: 10, bottom: 0 }}>
                <defs><linearGradient id="save" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#0f766e" stopOpacity={0.28} /><stop offset="95%" stopColor="#0f766e" stopOpacity={0} /></linearGradient><linearGradient id="repay" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#2563eb" stopOpacity={0.22} /><stop offset="95%" stopColor="#2563eb" stopOpacity={0} /></linearGradient></defs>
                <CartesianGrid strokeDasharray="4 4" stroke="#e7efec" vertical={false} />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: "#64748b" }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: "#94a3b8" }} axisLine={false} tickLine={false} tickFormatter={(value) => `${Math.round(value / 1000)}k`} />
                <Tooltip formatter={(value) => money(value)} contentStyle={{ borderRadius: 14, border: "1px solid #e2e8f0", boxShadow: "0 10px 25px rgba(0,0,0,.08)" }} />
                <Area type="monotone" dataKey="savings" name="Savings" stroke="#0f766e" strokeWidth={2.5} fill="url(#save)" />
                <Area type="monotone" dataKey="repayments" name="Repayments" stroke="#2563eb" strokeWidth={2.5} fill="url(#repay)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <h2 className="font-bold text-slate-900">Attention required</h2>
          <p className="mt-1 text-xs text-slate-500">Items that may affect financial follow-up</p>
          <div className="mt-5 space-y-3">
            {canApprove && <button onClick={() => routeTo("approvals")} className="flex w-full items-center justify-between rounded-2xl bg-amber-50 p-4 text-left"><span><span className="block text-xs font-semibold text-amber-800">Pending approvals</span><span className="mt-1 block text-2xl font-bold text-amber-900">{data.pendingApprovals}</span></span><ClipboardIcon /></button>}
            <div className="flex items-center justify-between rounded-2xl bg-rose-50 p-4"><span><span className="block text-xs font-semibold text-rose-800">Outstanding loan members</span><span className="mt-1 block text-2xl font-bold text-rose-900">{data.membersWithOutstandingLoans}</span></span><Landmark size={25} className="text-rose-600" /></div>
            {!isMember && <div className="flex items-center justify-between rounded-2xl bg-violet-50 p-4"><span><span className="block text-xs font-semibold text-violet-800">Left with active loan</span><span className="mt-1 block text-2xl font-bold text-violet-900">{data.leftMembersWithOutstandingLoans}</span></span><Users size={25} className="text-violet-600" /></div>}
          </div>
        </Card>
      </div>

      {canApprove && data.recentApprovals.length > 0 && <div className="mt-6"><div className="mb-3 flex items-center justify-between"><h2 className="font-bold text-slate-900">Recent pending approvals</h2><button onClick={() => routeTo("approvals")} className="text-xs font-semibold text-teal-700 hover:text-teal-900">View all</button></div><DataTable pageSize={5} rows={data.recentApprovals} columns={[
        { key: "reference", label: "Reference", className: "font-semibold" },
        { key: "description", label: "Transaction" },
        { key: "amount", label: "Amount", render: (row) => money(row.amount) },
        { key: "createdBy", label: "Prepared by" },
        { key: "submittedAt", label: "Submitted", render: (row) => dateTime(row.submittedAt) },
        { key: "status", label: "Status", render: () => <StatusBadge value="PENDING_APPROVAL" /> },
      ]} /></div>}
    </>
  );
}

function ClipboardIcon() {
  return <ClipboardCheckIcon />;
}

function ClipboardCheckIcon() {
  return <span className="grid size-10 place-items-center rounded-xl bg-amber-100 text-amber-700"><Activity size={20} /></span>;
}

