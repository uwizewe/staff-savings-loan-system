import { useEffect, useMemo, useState } from "react";
import { ArrowUpRight, CheckSquare, Coins, Plus, Send, WalletCards } from "lucide-react";
import { post } from "../api";
import { useAuth } from "../auth";
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, MoneyCell, PageHeader, SearchBox, StatusBadge, Tabs, textareaClass, useNotice } from "../components/ui";
import { useApiData } from "../hooks";
import { date, money, routeTo, thisMonth, today } from "../utils";

export default function SavingsPage({ route }) {
  const { user } = useAuth();
  return user.role === "MEMBER" ? <MemberSavings /> : <StaffSavings route={route} user={user} />;
}

function MemberSavings() {
  const { data, loading, error, reload } = useApiData("/savings");
  if (loading) return <Loading label="Loading your savings…" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  const balance = data.filter((item) => item.status === "APPROVED").reduce((sum, item) => sum + Number(item.amount) * (item.savingType === "WITHDRAWAL" ? -1 : 1), 0);
  return <><PageHeader title="My savings" description="Approved contributions, withdrawals and current balance." /><Card className="mb-6 bg-gradient-to-r from-teal-800 to-teal-600 text-white"><p className="text-xs font-semibold uppercase tracking-wider text-teal-100/70">Available savings</p><p className="mt-2 text-3xl font-bold">{money(balance)}</p></Card><SavingsTable rows={data} /></>;
}

function StaffSavings({ route, user }) {
  const tab = new URLSearchParams(route.split("?")[1] || "").get("tab") || "history";
  const savings = useApiData("/savings");
  const members = useApiData("/members");
  const batches = useApiData("/savings/batches");
  const notify = useNotice();
  const [search, setSearch] = useState("");
  const [form, setForm] = useState({ memberId: "", amount: "", transactionDate: today(), reference: "", description: "", submitNow: true });
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
  const filtered = savings.data.filter((item) => !search || [item.memberName, item.reference, item.savingType].some((value) => value?.toLowerCase().includes(search.toLowerCase())));
  const activeMembers = members.data.filter((member) => member.membershipStatus === "ACTIVE");
  const batchTotal = activeMembers.filter((member) => selected[member.id]).reduce((sum, member) => sum + Number(entries[member.id] || 0), 0);

  const saveIndividual = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      const endpoint = tab === "withdrawal" ? "/savings/withdrawals" : "/savings/individual";
      const created = await post(endpoint, { ...form, memberId: Number(form.memberId), amount: Number(form.amount) });
      if (form.submitNow) await post(`/savings/${created.id}/submit`);
      notify(form.submitNow ? "Savings transaction submitted" : "Savings draft saved");
      setForm({ memberId: "", amount: "", transactionDate: today(), reference: "", description: "", submitNow: true });
      savings.reload();
    } catch (requestError) { notify(requestError.message, "error"); }
    finally { setBusy(false); }
  };

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
      <PageHeader title="Savings management" description="Record individual or monthly contributions and withdrawals through approval." />
      <Tabs value={tab} onChange={(value) => routeTo(`savings?tab=${value}`)} items={[
        { value: "history", label: "Savings history" }, { value: "monthly", label: "Monthly savings" }, { value: "individual", label: "Individual savings" }, { value: "withdrawal", label: "Withdrawals" },
      ]} />

      {tab === "history" && <><div className="mb-4"><SearchBox value={search} onChange={setSearch} placeholder="Search member or reference" /></div><SavingsTable rows={filtered} canSubmit={canPrepare} onSubmit={submitDraft} /></>}

      {(tab === "individual" || tab === "withdrawal") && (canPrepare ? <div className="grid gap-6 xl:grid-cols-[.85fr_1.15fr]">
        <Card><div className="mb-5 flex items-center gap-3"><span className={`grid size-10 place-items-center rounded-xl ${tab === "withdrawal" ? "bg-rose-50 text-rose-700" : "bg-teal-50 text-teal-700"}`}>{tab === "withdrawal" ? <ArrowUpRight size={20} /> : <Plus size={20} />}</span><div><h2 className="font-bold text-slate-900">{tab === "withdrawal" ? "Record withdrawal" : "Record individual savings"}</h2><p className="text-xs text-slate-500">The transaction remains controlled by approval.</p></div></div>
          <form onSubmit={saveIndividual} className="space-y-4">
            <Field label="Member" required><select className={inputClass} value={form.memberId} onChange={(e) => setForm({ ...form, memberId: e.target.value })} required><option value="">Select member</option>{(tab === "withdrawal" ? members.data : activeMembers).map((member) => <option key={member.id} value={member.id}>{member.memberCode} — {member.fullName}</option>)}</select></Field>
            <div className="grid gap-4 sm:grid-cols-2"><Field label="Amount (RWF)" required><input className={inputClass} type="number" min="1" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required /></Field><Field label="Transaction date" required><input className={inputClass} type="date" value={form.transactionDate} onChange={(e) => setForm({ ...form, transactionDate: e.target.value })} required /></Field></div>
            <Field label="Reference" hint="Leave blank to generate automatically"><input className={inputClass} value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} /></Field>
            <Field label="Description"><textarea className={textareaClass} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
            <label className="flex items-center gap-2 text-xs font-medium text-slate-600"><input type="checkbox" className="size-4 accent-teal-700" checked={form.submitNow} onChange={(e) => setForm({ ...form, submitNow: e.target.checked })} />Submit immediately for approval</label>
            <Button className="w-full" type="submit" loading={busy}><Send size={16} />Save transaction</Button>
          </form>
        </Card>
        <div><h2 className="mb-3 font-bold text-slate-900">Recent {tab === "withdrawal" ? "withdrawals" : "individual savings"}</h2><SavingsTable rows={savings.data.filter((item) => item.savingType === (tab === "withdrawal" ? "WITHDRAWAL" : "INDIVIDUAL"))} canSubmit={canPrepare} onSubmit={submitDraft} /></div>
      </div> : <Card>You can review savings, but only an Initiator or Administrator can prepare a transaction.</Card>)}

      {tab === "monthly" && <div className="space-y-6">
        {canPrepare && <Card><div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div><h2 className="font-bold text-slate-900">Prepare monthly savings</h2><p className="text-xs text-slate-500">Select active members, verify amounts, then submit one controlled batch.</p></div><Field label="Contribution month"><input className={`${inputClass} w-44`} type="month" value={period} onChange={(e) => setPeriod(e.target.value)} /></Field></div>
          <div className="max-h-[420px] overflow-auto rounded-xl border border-slate-200"><table className="min-w-full divide-y divide-slate-100"><thead className="sticky top-0 bg-slate-50"><tr><th className="px-4 py-3 text-left text-xs text-slate-500">Include</th><th className="px-4 py-3 text-left text-xs text-slate-500">Member</th><th className="px-4 py-3 text-left text-xs text-slate-500">Amount (RWF)</th></tr></thead><tbody className="divide-y divide-slate-100">{activeMembers.map((member) => <tr key={member.id}><td className="px-4 py-3"><input type="checkbox" className="size-4 accent-teal-700" checked={Boolean(selected[member.id])} onChange={(e) => setSelected({ ...selected, [member.id]: e.target.checked })} /></td><td className="px-4 py-3 text-sm"><p className="font-semibold">{member.fullName}</p><p className="text-xs text-slate-400">{member.memberCode}</p></td><td className="px-4 py-3"><input className={`${inputClass} max-w-44`} type="number" min="1" disabled={!selected[member.id]} value={entries[member.id] ?? ""} onChange={(e) => setEntries({ ...entries, [member.id]: e.target.value })} /></td></tr>)}</tbody></table></div>
          <div className="mt-5 flex flex-col gap-3 rounded-2xl bg-teal-50 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-semibold text-teal-700">Batch total · {Object.values(selected).filter(Boolean).length} members</p><p className="mt-1 text-xl font-bold text-teal-900">{money(batchTotal)}</p></div><Button onClick={saveBatch} loading={busy}><CheckSquare size={16} />Create and submit batch</Button></div>
        </Card>}
        <div><h2 className="mb-3 font-bold text-slate-900">Monthly batches</h2><DataTable rows={batches.data} columns={[
          { key: "period", label: "Month", className: "font-semibold" }, { key: "totalAmount", label: "Batch total", render: (row) => <MoneyCell value={row.totalAmount} /> }, { key: "createdBy", label: "Prepared by" }, { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> }, { key: "actions", label: "Actions", render: (row) => canPrepare && ["DRAFT", "REJECTED"].includes(row.status) ? <Button size="sm" variant="secondary" onClick={() => submitDraft(row.id, true)}><Send size={14} />Submit</Button> : null },
        ]} /></div>
      </div>}
    </>
  );
}

function SavingsTable({ rows, canSubmit, onSubmit }) {
  return <DataTable rows={rows} columns={[
    { key: "transactionDate", label: "Date", render: (row) => date(row.transactionDate) },
    { key: "reference", label: "Reference", className: "font-semibold text-teal-800" },
    { key: "memberName", label: "Member" },
    { key: "savingType", label: "Type" },
    { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> },
    { key: "createdBy", label: "Recorded by" },
    { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> },
    ...(canSubmit ? [{ key: "actions", label: "Actions", render: (row) => ["DRAFT", "REJECTED"].includes(row.status) ? <Button size="sm" variant="secondary" onClick={() => onSubmit(row.id)}><Send size={14} />Submit</Button> : null }] : []),
  ]} />;
}

