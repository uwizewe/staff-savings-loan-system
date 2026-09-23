import LoanBreakdown from '../../loans/components/LoanBreakdown.jsx';
import { useRef, useState } from "react";
import { Check, X } from "lucide-react";
import { get, post } from "../../../services/api.js";
import { useAuth } from "../../auth/hooks/useAuth.jsx";
import { Button, DataTable, ErrorState, Field, Modal, MoneyCell, textareaClass, useNotice } from "../../../components/ui/index.jsx";

export default function ApprovalActions({ type, record, onDone, batchItemsPath }) {
  const { user } = useAuth();
  const [error, setError] = useState("");
  const [repayment, setRepayment] = useState(null);
  const [loanChange, setLoanChange] = useState(null);
  const [breakdown, setBreakdown] = useState(null);
  const [decision, setDecision] = useState(null);
  const [remarks, setRemarks] = useState("");
  const [batchDescription, setBatchDescription] = useState("");
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(false);
  const pending = useRef(false);
  const notify = useNotice();
  if (!["APPROVER", "ADMIN"].includes(user.role) || !record || (record.workflowStatus ?? record.status) !== "PENDING_APPROVAL") return null;

  const open = async (approve) => {
    if (pending.current) return;
    pending.current = true;
    setBusy(true); setError("");
    try {
      setItems(batchItemsPath ? await get(batchItemsPath) : []);
      if (batchItemsPath) {
        const batches = await get(batchItemsPath.replace(/\/[^/]+\/items$/, ""));
        setBatchDescription(batches.find(b => b.id === record.id)?.remarks || "");
      }
      if (["REPAYMENT", "FULL_SETTLEMENT"].includes(type)) {
        const payment = await get(`/loans/repayments/${record.id}`); setRepayment(payment); setItems([payment]);
      }
      if (type === "LOAN") {
        const loan = await get(`/loans/${record.id}`);
        const schedule = await get(`/loans/${record.id}/schedule`);
        setBreakdown({ principal: loan.approvedAmount || loan.requestedAmount, annualRate: loan.annualInterestRate, months: loan.repaymentMonths, interest: loan.totalInterest, monthlyInstallment: loan.monthlyInstallment, totalPayable: loan.totalPayable, rows: schedule.map(r=>({number:r.installmentNumber,dueDate:r.dueDate,openingBalance:r.openingBalance,principal:r.principalAmount,interest:r.interestAmount,amount:r.expectedAmount,closingBalance:r.closingBalance,paymentStatus:r.paymentStatus})) });
      }
      if (type === "LOAN_CHANGE") {
        const loan = await get(`/loans/${record.id}`);
        setLoanChange(loan);
        setBreakdown(approve ? await post(`/loans/${record.id}/adjustment-preview`, { type: loan.adjustmentType, amount: loan.adjustmentAmount, months: loan.adjustmentMonths, effectiveDate: loan.adjustmentDate, remarks: loan.adjustmentRemarks }) : null);
      }
      setRemarks("");
      setDecision(approve);
    } catch (error) { setError(error.message); notify(error.message, "error"); }
    finally { pending.current = false; setBusy(false); }
  };
  const submit = async (event) => {
    event.preventDefault();
    if (pending.current || decision === null || (!decision && !remarks.trim())) return;
    pending.current = true;
    setBusy(true);
    try {
      await post(`/approvals/${type}/${record.id}/decision`, { approve: decision, remarks: remarks.trim() });
      notify(decision ? "Approved successfully" : "Rejected successfully");
      setDecision(null);
      onDone();
    } catch (error) { notify(error.message, "error"); }
    finally { pending.current = false; setBusy(false); }
  };
  return <>
    <Button type="button" size="sm" variant="success" disabled={busy} onClick={() => open(true)}><Check size={14} />{batchItemsPath ? "Approve batch" : "Approve"}</Button>
    <Button type="button" size="sm" variant="danger" disabled={busy} onClick={() => open(false)}><X size={14} />{batchItemsPath ? "Reject batch" : "Reject"}</Button>
    <Modal open={decision !== null} onClose={() => { if (!pending.current) setDecision(null); }} title={`${decision ? "Approve" : "Reject"} ${batchItemsPath ? "monthly batch" : type === "LOAN_CHANGE" ? "loan change" : type === "LOAN" ? "loan application" : "transaction"}`} description={record.applicationNumber || record.reference || `Batch ${record.period}`} size="2xl">
      <form onSubmit={submit} className="space-y-4">{error && <ErrorState message={error} />}
        <div className="rounded-xl bg-teal-50 p-4"><p className="text-xs text-teal-700">{record.memberName || record.description || `Monthly batch · ${record.period}`}</p><p className="mt-1 text-xl font-bold text-teal-900"><MoneyCell value={record.requestedAmount ?? record.totalAmount ?? record.amount} /></p><p className="mt-2 text-xs text-slate-600">Prepared by {record.createdBy}</p></div>
        {batchDescription && <p className="whitespace-pre-wrap text-sm">Description: {batchDescription}</p>}
        {record.remarks && <p className="whitespace-pre-wrap text-sm">Description: {record.remarks}</p>}
        {loanChange && <><p className="text-sm">{loanChange.adjustmentType} · Additional principal: <MoneyCell value={loanChange.adjustmentAmount} /> · {loanChange.adjustmentMonths} installments · Effective {loanChange.adjustmentDate}</p><p className="text-sm">{loanChange.adjustmentRemarks}</p><LoanBreakdown value={breakdown} startDate={loanChange.adjustmentType === "TOP_UP" ? null : loanChange.adjustmentDate} /></>}
        {type === "LOAN" && <div className="text-sm text-slate-600"><p>{record.repaymentMonths} months · {record.annualInterestRate}% annual interest</p><p className="mt-2 whitespace-pre-wrap">{record.purpose}</p></div>}
        {type === "LOAN" && <LoanBreakdown value={breakdown} />}
        {repayment && <p className="text-sm">{repayment.remarks}{type === "FULL_SETTLEMENT" && " · Remaining principal plus one monthly term of interest; future interest is waived."}</p>}
        {batchItemsPath && type !== "REPAYMENT_BATCH" && <><p className="text-sm text-amber-800">This decision applies to the entire batch of {items.length} transactions.</p><DataTable rows={items} columns={[{ key: "memberName", label: "Member" }, { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> }]} /></>}
        {(type === "REPAYMENT_BATCH" || repayment) && <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">{[["Staff", new Set(items.map(r=>r.memberId)).size], ["Principal", items.reduce((s,r)=>s+Number(r.principalPaid||0),0)], ["Interest",items.reduce((s,r)=>s+Number(r.interestPaid||0),0)], ["Total repayment",items.reduce((s,r)=>s+Number(r.amount||0),0)]].map(([label,value])=><div key={label} className="rounded-xl bg-slate-50 p-3"><p className="text-xs text-slate-500">{label}</p><p className="font-bold">{label === "Staff" ? value : <MoneyCell value={value}/>}</p></div>)}</div>
          <DataTable rows={items} pageSize={100} columns={[{key:"memberCode",label:"Staff ID"},{key:"memberName",label:"Staff name"},{key:"loanNumber",label:"Loan"},{key:"installmentNumber",label:"Installment"}, ...[["principalPaid","Principal"],["interestPaid","Interest"],["chargesPaid","Charges"],["amount","Amount paid"],["outstandingBefore","Outstanding before"]].map(([key,label])=>({key,label,render:r=><MoneyCell value={r[key]}/>}))]}/>
        </>}
        <Field label="Decision remarks" required={decision === false}><textarea className={textareaClass} value={remarks} onChange={(event) => setRemarks(event.target.value)} required={decision === false} maxLength={500} placeholder={decision ? "Optional review note" : "Explain the reason for rejection"} /></Field>
        <p className="text-xs text-slate-500">The creator cannot approve their own transaction. Your decision is recorded in the audit history.</p>
        <div className="flex justify-end gap-2"><Button type="button" variant="secondary" disabled={busy} onClick={() => setDecision(null)}>Cancel</Button><Button type="submit" variant={decision ? "success" : "danger"} loading={busy} disabled={busy || (decision === false && !remarks.trim())}>{decision ? "Confirm approval" : "Confirm rejection"}</Button></div>
      </form>
    </Modal>
  </>;
}
