import { useState } from "react";
import { ArrowDownLeft, ArrowUpRight, Send } from "lucide-react";
import { post } from "../../../services/api.js";
import { useAuth } from "../../auth/hooks/useAuth.jsx";
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, MoneyCell, PageHeader, StatusBadge, Tabs, textareaClass, useNotice } from "../../../components/ui/index.jsx";
import { useApiData } from "../../../hooks/useApiData.js";
import { date, routeTo, today } from "../../../utils/index.js";

export default function FinancePage({ route }) {
  const { user } = useAuth();
  const tab = new URLSearchParams(route.split("?")[1] || "").get("tab") || "history";
  const transactions = useApiData("/finance");
  const categories = useApiData("/admin/categories?activeOnly=true");
  const notify = useNotice();
  const [form, setForm] = useState({ categoryId: "", transactionDate: today(), description: "", amount: "", reference: "", supportingReference: "", submitNow: true });
  const [busy, setBusy] = useState(false);
  const canPrepare = ["INITIATOR", "ADMIN"].includes(user.role);

  if (transactions.loading || categories.loading) return <Loading label="Loading income and expenses…" />;
  const error = transactions.error || categories.error;
  if (error) return <ErrorState message={error} onRetry={() => { transactions.reload(); categories.reload(); }} />;

  const type = tab === "expense" ? "EXPENSE" : "INCOME";
  const rows = tab === "history" ? transactions.data : transactions.data.filter((item) => item.financeType === type);
  const matchingCategories = categories.data.filter((item) => item.categoryType === type);

  const save = async (event) => {
    event.preventDefault(); setBusy(true);
    try {
      const created = await post("/finance", { ...form, financeType: type, categoryId: Number(form.categoryId), amount: Number(form.amount) });
      if (form.submitNow) await post(`/finance/${created.id}/submit`);
      notify(form.submitNow ? `${type === "INCOME" ? "Income" : "Expense"} submitted` : "Transaction draft saved");
      setForm({ categoryId: "", transactionDate: today(), description: "", amount: "", reference: "", supportingReference: "", submitNow: true });
      transactions.reload();
    } catch (e) { notify(e.message, "error"); } finally { setBusy(false); }
  };

  const submitDraft = async (id) => { try { await post(`/finance/${id}/submit`); notify("Transaction submitted"); transactions.reload(); } catch (e) { notify(e.message, "error"); } };

  return (
    <>
      <PageHeader title="Income & expenses" description="Record association operating transactions separately from member savings and loan repayments." />
      <Tabs value={tab} onChange={(value) => routeTo(`finance?tab=${value}`)} items={[{ value: "history", label: "Transaction history" }, { value: "income", label: "Income" }, { value: "expense", label: "Expenses" }]} />
      {tab !== "history" && canPrepare && <div className="mb-6 grid gap-6 xl:grid-cols-[.8fr_1.2fr]">
        <Card><div className="mb-5 flex items-center gap-3"><span className={`grid size-10 place-items-center rounded-xl ${type === "INCOME" ? "bg-emerald-50 text-emerald-700" : "bg-rose-50 text-rose-700"}`}>{type === "INCOME" ? <ArrowDownLeft size={20} /> : <ArrowUpRight size={20} />}</span><div><h2 className="font-bold">Record {type.toLowerCase()}</h2><p className="text-xs text-slate-500">Save evidence references where available.</p></div></div><form onSubmit={save} className="space-y-4"><Field label="Category" required><select className={inputClass} value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required><option value="">Select category</option>{matchingCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></Field><div className="grid gap-4 sm:grid-cols-2"><Field label="Amount (RWF)" required><input type="number" min="1" className={inputClass} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required /></Field><Field label="Date" required><input type="date" className={inputClass} value={form.transactionDate} onChange={(e) => setForm({ ...form, transactionDate: e.target.value })} required /></Field></div><Field label="Description" required><textarea className={textareaClass} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required /></Field><Field label="Transaction reference"><input className={inputClass} value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} placeholder="Generated when blank" /></Field><Field label="Supporting document/reference"><input className={inputClass} value={form.supportingReference} onChange={(e) => setForm({ ...form, supportingReference: e.target.value })} /></Field><label className="flex items-center gap-2 text-xs font-medium text-slate-600"><input type="checkbox" className="size-4 accent-teal-700" checked={form.submitNow} onChange={(e) => setForm({ ...form, submitNow: e.target.checked })} />Submit immediately for approval</label><Button className="w-full" type="submit" loading={busy}><Send size={16} />Save {type.toLowerCase()}</Button></form></Card>
        <div><h2 className="mb-3 font-bold">Recent {type.toLowerCase()} transactions</h2><FinanceTable rows={rows} canSubmit={canPrepare} onSubmit={submitDraft} /></div>
      </div>}
      {(tab === "history" || !canPrepare) && <FinanceTable rows={rows} canSubmit={canPrepare} onSubmit={submitDraft} />}
    </>
  );
}

function FinanceTable({ rows, canSubmit, onSubmit }) {
  return <DataTable rows={rows} columns={[{ key: "transactionDate", label: "Date", render: (row) => date(row.transactionDate) }, { key: "reference", label: "Reference", className: "font-semibold text-teal-800" }, { key: "financeType", label: "Type" }, { key: "category", label: "Category" }, { key: "description", label: "Description", className: "max-w-64 truncate" }, { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> }, { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> }, ...(canSubmit ? [{ key: "actions", label: "Actions", render: (row) => ["DRAFT", "REJECTED"].includes(row.status) ? <Button size="sm" variant="secondary" onClick={() => onSubmit(row.id)}><Send size={14} />Submit</Button> : null }] : [])]} />;
}

