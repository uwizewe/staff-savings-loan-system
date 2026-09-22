import { useState } from "react";
import { Check, ClipboardCheck, X } from "lucide-react";
import { post } from "../api";
import { Button, Card, DataTable, ErrorState, Loading, Modal, MoneyCell, PageHeader, StatusBadge, textareaClass, useNotice } from "../components/ui";
import { useApiData } from "../hooks";
import { dateTime } from "../utils";

export default function ApprovalsPage() {
  const { data, loading, error, reload } = useApiData("/approvals");
  const [decision, setDecision] = useState(null);
  const [remarks, setRemarks] = useState("");
  const [busy, setBusy] = useState(false);
  const notify = useNotice();

  if (loading) return <Loading label="Loading approvals…" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;

  const submit = async () => {
    setBusy(true);
    try {
      await post(`/approvals/${decision.item.type}/${decision.item.id}/decision`, { approve: decision.approve, remarks });
      notify(decision.approve ? "Transaction approved" : "Transaction rejected");
      setDecision(null); setRemarks(""); reload();
    } catch (requestError) { notify(requestError.message, "error"); }
    finally { setBusy(false); }
  };

  return (
    <>
      <PageHeader title="Pending approvals" description="Independent maker–checker review for all submitted financial transactions." />
      {!data.length ? <Card className="grid min-h-64 place-items-center text-center"><div><span className="mx-auto grid size-14 place-items-center rounded-2xl bg-emerald-50 text-emerald-700"><ClipboardCheck size={28} /></span><h2 className="mt-4 font-bold text-slate-900">Everything is up to date</h2><p className="mt-1 text-sm text-slate-500">There are no transactions waiting for approval.</p></div></Card> : <DataTable rows={data} keyField={(row) => `${row.type}-${row.id}`} columns={[
        { key: "type", label: "Type", render: (row) => <span className="rounded-lg bg-slate-100 px-2 py-1 text-[11px] font-bold text-slate-600">{row.type.replaceAll("_", " ")}</span> },
        { key: "reference", label: "Reference", className: "font-semibold text-teal-800" }, { key: "description", label: "Transaction" }, { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> }, { key: "createdBy", label: "Prepared by" }, { key: "submittedAt", label: "Submitted", render: (row) => dateTime(row.submittedAt) }, { key: "status", label: "Status", render: () => <StatusBadge value="PENDING_APPROVAL" /> }, { key: "actions", label: "Decision", render: (row) => <div className="flex gap-2"><Button size="sm" variant="success" onClick={() => setDecision({ item: row, approve: true })}><Check size={14} />Approve</Button><Button size="sm" variant="danger" onClick={() => setDecision({ item: row, approve: false })}><X size={14} />Reject</Button></div> },
      ]} />}
      <Modal open={Boolean(decision)} onClose={() => setDecision(null)} title={decision?.approve ? "Approve transaction" : "Reject transaction"} description={decision ? `${decision.item.reference} · ${decision.item.description}` : ""} size="sm"><div className="space-y-4"><div className={`rounded-2xl p-4 ${decision?.approve ? "bg-emerald-50 text-emerald-800" : "bg-rose-50 text-rose-800"}`}><p className="text-xs font-semibold">Transaction amount</p><p className="mt-1 text-xl font-bold"><MoneyCell value={decision?.item.amount} /></p></div><label className="block"><span className="mb-1.5 block text-xs font-semibold text-slate-700">Decision remarks {decision && !decision.approve && <span className="text-rose-500">*</span>}</span><textarea className={textareaClass} value={remarks} onChange={(event) => setRemarks(event.target.value)} placeholder="Add a clear reason or review note" /></label><p className="text-xs leading-5 text-slate-500">Your name and decision time will be written permanently to the approval and audit history.</p><div className="flex justify-end gap-2"><Button variant="secondary" onClick={() => setDecision(null)}>Cancel</Button><Button variant={decision?.approve ? "success" : "danger"} loading={busy} disabled={!decision?.approve && !remarks.trim()} onClick={submit}>{decision?.approve ? "Confirm approval" : "Confirm rejection"}</Button></div></div></Modal>
    </>
  );
}

