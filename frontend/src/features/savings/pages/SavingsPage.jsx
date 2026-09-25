import SavingsDashboardPage from './SavingsDashboardPage.jsx';
import SavingsTable from '../components/SavingsTable.jsx';
import ApprovalActions from "../../approvals/components/ApprovalActions.jsx";
import { useEffect, useState } from "react";
import { CheckSquare, Send } from "lucide-react";
import { post } from "../../../services/api.js";
import { useAuth } from "../../auth/hooks/useAuth.jsx";
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, MoneyCell, PageHeader, StatusBadge, useNotice } from "../../../components/ui/index.jsx";
import { useApiData } from "../../../hooks/useApiData.js";
import { money, routeTo, thisMonth } from "../../../utils/index.js";

export default function SavingsPage({ route }) {
  const { user } = useAuth();
  return user.role === "MEMBER" || new URLSearchParams(route.split("?")[1]).get("view")==="dashboard" ? <SavingsDashboardPage route={route}/> : <StaffSavings route={route} user={user} />;
}

function StaffSavings({ route, user }) {
  const params=new URLSearchParams(route.split("?")[1]);
  const view=params.get("view") || params.get("tab") || "members";
  const savings = useApiData("/savings");
  const members = useApiData("/members");
  const batches = useApiData("/savings/batches");
  const notify = useNotice();
  const [period, setPeriod] = useState(thisMonth());
  const [entries, setEntries] = useState({});
  const [selected, setSelected] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!members.data) return;
    const active = members.data.filter((member) => member.membershipStatus === "ACTIVE");
    setEntries(Object.fromEntries(active.map((member) => [member.id, member.monthlySavingAmount])));
    setSelected(Object.fromEntries(active.map((member) => [member.id, true])));
  }, [members.data]);

  if (savings.loading || members.loading || batches.loading) return <Loading label="Loading savings workspace…" />;
  const error = savings.error || members.error || batches.error;
  if (error) return <ErrorState message={error} onRetry={() => { savings.reload(); members.reload(); batches.reload(); }} />;

  const canPrepare = ["INITIATOR", "ADMIN"].includes(user.role);
  const activeMembers = members.data.filter((member) => member.membershipStatus === "ACTIVE");
  const batchTotal = activeMembers.filter((member) => selected[member.id]).reduce((sum, member) => sum + Number(entries[member.id] || 0), 0);

  const saveBatch = async () => {
    const items = activeMembers.filter((member) => selected[member.id]).map((member) => ({ memberId: member.id, amount: Number(entries[member.id]) }));
    if (!items.length) return notify("Select at least one member", "error");
    setBusy(true);
    try {
      const created = await post("/savings/batches", { period, items, remarks: `Monthly savings for ${period}` });
      await post(`/savings/batches/${created.id}/submit`);
      notify("Monthly savings batch submitted");
      savings.reload(); batches.reload();
    } catch (requestError) { notify(requestError.message, "error"); }
    finally { setBusy(false); }
  };

  const submitDraft = async (id, batch = false) => {
    try { await post(batch ? `/savings/batches/${id}/submit` : `/savings/${id}/submit`); notify("Submitted for approval"); savings.reload(); batches.reload(); }
    catch (requestError) { notify(requestError.message, "error"); }
  };

  return (
    <>
      <PageHeader title={view==="monthly"?"Monthly Saving":view==="history"?"Savings History":"Members Saving"} description="Member savings, monthly contributions and transaction history." />
      {!['history','monthly'].includes(view) && <DataTable label="Members saving" rows={members.data} columns={[
        {key:'memberCode',label:'Member ID'},{key:'fullName',label:'Member Name'},
        {key:'monthlySavingAmount',label:'Monthly Saving',render:r=><MoneyCell value={r.monthlySavingAmount}/>},
        {key:'totalSavings',label:'Total Saving',render:r=><MoneyCell value={r.totalSavings}/>},
        {key:'membershipStatus',label:'Status',render:r=><StatusBadge value={r.membershipStatus}/>},
        {key:'actions',label:'Action',render:r=><Button variant="secondary" size="sm" onClick={()=>routeTo(`savings?view=dashboard&id=${r.id}`)}>View Savings History</Button>}
      ]}/>}

      {view === "history" && <><SavingsTable rows={savings.data} canSubmit={canPrepare} onSubmit={submitDraft} canApprove={["APPROVER", "ADMIN"].includes(user.role)} batches={batches.data} onDecision={() => { savings.reload(); batches.reload(); members.reload(); }} /></>}

      {view === "monthly" && <div className="space-y-6">
        {canPrepare && <Card><div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div><h2 className="font-bold text-slate-900">Prepare monthly savings</h2><p className="text-xs text-slate-500">Select active members, verify amounts, then submit one controlled batch.</p></div><Field label="Contribution month"><input className={`${inputClass} w-44`} type="month" value={period} onChange={(e) => setPeriod(e.target.value)} /></Field></div>
          <DataTable label="Monthly savings" rows={activeMembers} columns={[
            {key:'include',label:'Include',render:member=><input aria-label={`Include ${member.fullName}`} type="checkbox" checked={Boolean(selected[member.id])} onChange={e=>setSelected({...selected,[member.id]:e.target.checked})}/>},
            {key:'memberCode',label:'Member ID'},{key:'fullName',label:'Member Name'},
            {key:'payment',label:'Amount',render:member=><input aria-label={`Amount for ${member.fullName}`} className={`${inputClass} w-40`} type="number" min="0.01" step="0.01" disabled={!selected[member.id]} value={entries[member.id]??''} onChange={e=>setEntries({...entries,[member.id]:e.target.value})}/>}
          ]}/>
          <div className="mt-5 flex flex-col gap-3 rounded-2xl bg-teal-50 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-semibold text-teal-700">Batch total · {Object.values(selected).filter(Boolean).length} members</p><p className="mt-1 text-xl font-bold text-teal-900">{money(batchTotal)}</p></div><Button onClick={saveBatch} loading={busy}><CheckSquare size={16} />Create and submit batch</Button></div>
        </Card>}
        <div><h2 className="mb-3 font-bold text-slate-900">Monthly batches</h2><DataTable rows={batches.data} columns={[
          { key: "period", label: "Month", className: "font-semibold" }, { key: "totalAmount", label: "Batch total", render: (row) => <MoneyCell value={row.totalAmount} /> }, { key: "createdBy", label: "Prepared by" }, { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> }, { key: "actions", label: "Actions", render: (row) => <div className="flex gap-2">{canPrepare && ["DRAFT", "REJECTED"].includes(row.status) && <Button size="sm" variant="secondary" onClick={() => submitDraft(row.id, true)}><Send size={14} />Submit</Button>}<ApprovalActions type="SAVINGS_BATCH" record={row} batchItemsPath={`/savings/batches/${row.id}/items`} onDone={() => { savings.reload(); batches.reload(); members.reload(); }} /></div> },
        ]} /></div>
      </div>}
    </>
  );
}

