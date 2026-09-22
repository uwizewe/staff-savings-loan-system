import { useEffect, useMemo, useState } from "react";
import { CalendarDays, Landmark, ListOrdered, Plus, Send, WalletCards } from "lucide-react";
import { get, post } from "../api";
import { useAuth } from "../auth";
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, Modal, MoneyCell, PageHeader, SearchBox, StatusBadge, Tabs, textareaClass, useNotice } from "../components/ui";
import { useApiData } from "../hooks";
import { date, money, routeTo, thisMonth, today } from "../utils";

const loanBlank = { memberId: "", applicationDate: today(), requestedAmount: "", annualInterestRate: "12", repaymentMonths: "12", purpose: "", remarks: "" };
const repayBlank = { loanId: "", amount: "", paymentDate: today(), reference: "", remarks: "", submitNow: true };

export default function LoansPage({ route }) {
  const { user } = useAuth();
  return user.role === "MEMBER" ? <MemberLoans /> : <StaffLoans route={route} user={user} />;
}

function MemberLoans() {
  const loans = useApiData("/loans");
  const repayments = useApiData("/loans/repayments");
  const [schedule, setSchedule] = useState(null);
  const notify = useNotice();
  if (loans.loading || repayments.loading) return <Loading label="Loading your loans…" />;
  const error = loans.error || repayments.error;
  if (error) return <ErrorState message={error} onRetry={() => { loans.reload(); repayments.reload(); }} />;
  const openSchedule = async (loan) => { try { setSchedule({ loan, rows: await get(`/loans/${loan.id}/schedule`) }); } catch (e) { notify(e.message, "error"); } };
  return <><PageHeader title="My loans" description="Applications, active balances, schedules and repayments." /><LoanTable rows={loans.data} onSchedule={openSchedule} /><h2 className="mb-3 mt-7 font-bold text-slate-900">Repayment history</h2><RepaymentTable rows={repayments.data} /><ScheduleModal schedule={schedule} onClose={() => setSchedule(null)} /></>;
}

function StaffLoans({ route, user }) {
  const tab = new URLSearchParams(route.split("?")[1] || "").get("tab") || "applications";
  const loans = useApiData("/loans");
  const repayments = useApiData("/loans/repayments");
  const members = useApiData("/members");
  const batches = useApiData("/loans/repayment-batches");
  const notify = useNotice();
  const [loanModal, setLoanModal] = useState(false);
  const [repayModal, setRepayModal] = useState(false);
  const [loanForm, setLoanForm] = useState(loanBlank);
  const [repayForm, setRepayForm] = useState(repayBlank);
  const [schedule, setSchedule] = useState(null);
  const [disbursement, setDisbursement] = useState(null);
  const [disbursementForm, setDisbursementForm] = useState({ disbursementDate: today(), reference: "" });
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState("");
  const [period, setPeriod] = useState(thisMonth());
  const [monthly, setMonthly] = useState({});
  const [selected, setSelected] = useState({});

  const canPrepare = ["INITIATOR", "ADMIN"].includes(user.role);
  const activeLoans = (loans.data || []).filter((loan) => loan.loanStatus === "ACTIVE");

  useEffect(() => {
    setMonthly(Object.fromEntries(activeLoans.map((loan) => [loan.id, Math.min(Number(loan.monthlyInstallment || 0), Number(loan.outstandingBalance || 0))])));
    setSelected(Object.fromEntries(activeLoans.map((loan) => [loan.id, true])));
  }, [loans.data]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loans.loading || repayments.loading || members.loading || batches.loading) return <Loading label="Loading loan workspace…" />;
  const error = loans.error || repayments.error || members.error || batches.error;
  if (error) return <ErrorState message={error} onRetry={() => { loans.reload(); repayments.reload(); members.reload(); batches.reload(); }} />;

  const filteredLoans = loans.data.filter((loan) => {
    const match = !search || [loan.memberName, loan.applicationNumber, loan.purpose].some((value) => value?.toLowerCase().includes(search.toLowerCase()));
    return match && (tab !== "active" || loan.loanStatus === "ACTIVE");
  });

  const createLoan = async (event) => {
    event.preventDefault(); setBusy(true);
    try {
      const created = await post("/loans", { ...loanForm, memberId: Number(loanForm.memberId), requestedAmount: Number(loanForm.requestedAmount), annualInterestRate: Number(loanForm.annualInterestRate), repaymentMonths: Number(loanForm.repaymentMonths) });
      await post(`/loans/${created.id}/submit`);
      notify("Loan application submitted"); setLoanModal(false); setLoanForm(loanBlank); loans.reload();
    } catch (e) { notify(e.message, "error"); } finally { setBusy(false); }
  };

  const createRepayment = async (event) => {
    event.preventDefault(); setBusy(true);
    try {
      const created = await post("/loans/repayments", { ...repayForm, loanId: Number(repayForm.loanId), amount: Number(repayForm.amount) });
      if (repayForm.submitNow) await post(`/loans/repayments/${created.id}/submit`);
      notify(repayForm.submitNow ? "Repayment submitted" : "Repayment draft saved"); setRepayModal(false); setRepayForm(repayBlank); repayments.reload();
    } catch (e) { notify(e.message, "error"); } finally { setBusy(false); }
  };

  const submitLoan = async (id) => { try { await post(`/loans/${id}/submit`); notify("Loan submitted"); loans.reload(); } catch (e) { notify(e.message, "error"); } };
  const submitRepayment = async (id) => { try { await post(`/loans/repayments/${id}/submit`); notify("Repayment submitted"); repayments.reload(); } catch (e) { notify(e.message, "error"); } };
  const openSchedule = async (loan) => { try { setSchedule({ loan, rows: await get(`/loans/${loan.id}/schedule`) }); } catch (e) { notify(e.message, "error"); } };
  const saveDisbursement = async (event) => { event.preventDefault(); setBusy(true); try { await post(`/loans/${disbursement.id}/disburse`, disbursementForm); notify("Loan disbursed and schedule generated"); setDisbursement(null); loans.reload(); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); } };

  const createMonthly = async () => {
    const items = activeLoans.filter((loan) => selected[loan.id]).map((loan) => ({ loanId: loan.id, amount: Number(monthly[loan.id]) }));
    if (!items.length) return notify("Select at least one active loan", "error");
    setBusy(true); try { const created = await post("/loans/repayment-batches", { period, items, remarks: `Monthly loan repayments for ${period}` }); await post(`/loans/repayment-batches/${created.id}/submit`); notify("Monthly repayment batch submitted"); batches.reload(); repayments.reload(); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); }
  };

  const submitBatch = async (id) => { try { await post(`/loans/repayment-batches/${id}/submit`); notify("Batch submitted"); batches.reload(); } catch (e) { notify(e.message, "error"); } };

  return (
    <>
      <PageHeader title="Loan management" description="Manage applications, approvals, disbursements, schedules and repayments." actions={canPrepare && <><Button variant="secondary" onClick={() => setRepayModal(true)}><WalletCards size={16} />Record repayment</Button><Button onClick={() => setLoanModal(true)}><Plus size={16} />New loan application</Button></>} />
      <Tabs value={tab} onChange={(value) => routeTo(`loans?tab=${value}`)} items={[{ value: "applications", label: "Loan applications" }, { value: "active", label: "Active loans" }, { value: "repayments", label: "Repayment history" }, { value: "monthly", label: "Monthly repayments" }]} />
      {(tab === "applications" || tab === "active") && <><div className="mb-4"><SearchBox value={search} onChange={setSearch} placeholder="Search loan or member" /></div><LoanTable rows={filteredLoans} canPrepare={canPrepare} onSubmit={submitLoan} onSchedule={openSchedule} onDisburse={(loan) => { setDisbursement(loan); setDisbursementForm({ disbursementDate: today(), reference: "" }); }} /></>}
      {tab === "repayments" && <RepaymentTable rows={repayments.data} canPrepare={canPrepare} onSubmit={submitRepayment} />}
      {tab === "monthly" && <div className="space-y-6">{canPrepare && <Card><div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div><h2 className="font-bold">Prepare monthly repayments</h2><p className="text-xs text-slate-500">Expected installments are loaded from every active loan.</p></div><Field label="Repayment month"><input type="month" className={`${inputClass} w-44`} value={period} onChange={(e) => setPeriod(e.target.value)} /></Field></div><div className="max-h-[420px] overflow-auto rounded-xl border border-slate-200"><table className="min-w-full divide-y divide-slate-100"><thead className="sticky top-0 bg-slate-50"><tr><th className="px-4 py-3 text-left text-xs text-slate-500">Include</th><th className="px-4 py-3 text-left text-xs text-slate-500">Loan / member</th><th className="px-4 py-3 text-left text-xs text-slate-500">Outstanding</th><th className="px-4 py-3 text-left text-xs text-slate-500">Amount</th></tr></thead><tbody className="divide-y divide-slate-100">{activeLoans.map((loan) => <tr key={loan.id}><td className="px-4 py-3"><input type="checkbox" className="size-4 accent-teal-700" checked={Boolean(selected[loan.id])} onChange={(e) => setSelected({ ...selected, [loan.id]: e.target.checked })} /></td><td className="px-4 py-3 text-sm"><p className="font-semibold">{loan.memberName}</p><p className="text-xs text-slate-400">{loan.applicationNumber}</p></td><td className="px-4 py-3 text-sm font-semibold">{money(loan.outstandingBalance)}</td><td className="px-4 py-3"><input type="number" min="0.01" step="0.01" max={loan.outstandingBalance} className={`${inputClass} max-w-44`} disabled={!selected[loan.id]} value={monthly[loan.id] ?? ""} onChange={(e) => setMonthly({ ...monthly, [loan.id]: e.target.value })} /></td></tr>)}</tbody></table>{!activeLoans.length && <p className="p-8 text-center text-sm text-slate-400">No active loans</p>}</div><div className="mt-5 flex flex-col gap-3 rounded-2xl bg-teal-50 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-semibold text-teal-700">Batch total</p><p className="mt-1 text-xl font-bold text-teal-900">{money(activeLoans.filter((loan) => selected[loan.id]).reduce((sum, loan) => sum + Number(monthly[loan.id] || 0), 0))}</p></div><Button onClick={createMonthly} loading={busy}><Send size={16} />Create and submit batch</Button></div></Card>}
        <div><h2 className="mb-3 font-bold">Repayment batches</h2><DataTable rows={batches.data} columns={[{ key: "period", label: "Month", className: "font-semibold" }, { key: "totalAmount", label: "Total", render: (row) => <MoneyCell value={row.totalAmount} /> }, { key: "createdBy", label: "Prepared by" }, { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> }, ...(canPrepare ? [{ key: "actions", label: "Actions", render: (row) => ["DRAFT", "REJECTED"].includes(row.status) ? <Button size="sm" variant="secondary" onClick={() => submitBatch(row.id)}><Send size={14} />Submit</Button> : null }] : [])]} /></div>
      </div>}

      <Modal open={loanModal} onClose={() => setLoanModal(false)} title="New loan application" description="The application will be submitted to an Approver."><form onSubmit={createLoan} className="grid gap-4 sm:grid-cols-2"><Field label="Member" required className="sm:col-span-2"><select className={inputClass} value={loanForm.memberId} onChange={(e) => setLoanForm({ ...loanForm, memberId: e.target.value })} required><option value="">Select active member</option>{members.data.filter((m) => m.membershipStatus === "ACTIVE").map((m) => <option key={m.id} value={m.id}>{m.memberCode} — {m.fullName}</option>)}</select></Field><Field label="Application date" required><input type="date" className={inputClass} value={loanForm.applicationDate} onChange={(e) => setLoanForm({ ...loanForm, applicationDate: e.target.value })} required /></Field><Field label="Requested amount (RWF)" required><input type="number" min="1" className={inputClass} value={loanForm.requestedAmount} onChange={(e) => setLoanForm({ ...loanForm, requestedAmount: e.target.value })} required /></Field><Field label="Annual interest (%)" required><input type="number" min="0" step="0.01" className={inputClass} value={loanForm.annualInterestRate} onChange={(e) => setLoanForm({ ...loanForm, annualInterestRate: e.target.value })} required /></Field><Field label="Repayment months" required><input type="number" min="1" max="120" className={inputClass} value={loanForm.repaymentMonths} onChange={(e) => setLoanForm({ ...loanForm, repaymentMonths: e.target.value })} required /></Field><Field label="Loan purpose" required className="sm:col-span-2"><textarea className={textareaClass} value={loanForm.purpose} onChange={(e) => setLoanForm({ ...loanForm, purpose: e.target.value })} required /></Field><Field label="Remarks" className="sm:col-span-2"><textarea className={textareaClass} value={loanForm.remarks} onChange={(e) => setLoanForm({ ...loanForm, remarks: e.target.value })} /></Field><div className="flex justify-end gap-2 border-t border-slate-100 pt-4 sm:col-span-2"><Button type="button" variant="secondary" onClick={() => setLoanModal(false)}>Cancel</Button><Button type="submit" loading={busy}><Send size={16} />Submit application</Button></div></form></Modal>

      <Modal open={repayModal} onClose={() => setRepayModal(false)} title="Record loan repayment"><form onSubmit={createRepayment} className="space-y-4"><Field label="Active loan" required><select className={inputClass} value={repayForm.loanId} onChange={(e) => { const loan = activeLoans.find((item) => item.id === Number(e.target.value)); setRepayForm({ ...repayForm, loanId: e.target.value, amount: loan ? Math.min(Number(loan.monthlyInstallment), Number(loan.outstandingBalance)) : "" }); }} required><option value="">Select loan</option>{activeLoans.map((loan) => <option key={loan.id} value={loan.id}>{loan.applicationNumber} — {loan.memberName} ({money(loan.outstandingBalance)})</option>)}</select></Field><div className="grid gap-4 sm:grid-cols-2"><Field label="Amount (RWF)" required><input type="number" min="0.01" step="0.01" className={inputClass} value={repayForm.amount} onChange={(e) => setRepayForm({ ...repayForm, amount: e.target.value })} required /></Field><Field label="Payment date" required><input type="date" className={inputClass} value={repayForm.paymentDate} onChange={(e) => setRepayForm({ ...repayForm, paymentDate: e.target.value })} required /></Field></div><Field label="Reference"><input className={inputClass} value={repayForm.reference} onChange={(e) => setRepayForm({ ...repayForm, reference: e.target.value })} placeholder="Generated when blank" /></Field><Field label="Remarks"><textarea className={textareaClass} value={repayForm.remarks} onChange={(e) => setRepayForm({ ...repayForm, remarks: e.target.value })} /></Field><label className="flex items-center gap-2 text-xs font-medium text-slate-600"><input type="checkbox" className="size-4 accent-teal-700" checked={repayForm.submitNow} onChange={(e) => setRepayForm({ ...repayForm, submitNow: e.target.checked })} />Submit immediately for approval</label><div className="flex justify-end gap-2 border-t border-slate-100 pt-4"><Button type="button" variant="secondary" onClick={() => setRepayModal(false)}>Cancel</Button><Button type="submit" loading={busy}>Save repayment</Button></div></form></Modal>

      <Modal open={Boolean(disbursement)} onClose={() => setDisbursement(null)} title="Disburse approved loan" description={disbursement ? `${disbursement.applicationNumber} · ${disbursement.memberName}` : ""} size="sm"><form onSubmit={saveDisbursement} className="space-y-4"><Field label="Disbursement date" required><input type="date" className={inputClass} value={disbursementForm.disbursementDate} onChange={(e) => setDisbursementForm({ ...disbursementForm, disbursementDate: e.target.value })} required /></Field><Field label="Bank / payment reference" required><input className={inputClass} value={disbursementForm.reference} onChange={(e) => setDisbursementForm({ ...disbursementForm, reference: e.target.value })} required /></Field><p className="rounded-xl bg-blue-50 p-3 text-xs text-blue-700">Disbursement activates the loan and generates its complete repayment schedule.</p><div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => setDisbursement(null)}>Cancel</Button><Button type="submit" loading={busy}>Confirm disbursement</Button></div></form></Modal>
      <ScheduleModal schedule={schedule} onClose={() => setSchedule(null)} />
    </>
  );
}

function LoanTable({ rows, canPrepare, onSubmit, onSchedule, onDisburse }) {
  return <DataTable rows={rows} columns={[
    { key: "applicationNumber", label: "Loan number", className: "font-semibold text-teal-800" }, { key: "memberName", label: "Member" }, { key: "applicationDate", label: "Applied", render: (row) => date(row.applicationDate) }, { key: "requestedAmount", label: "Requested", render: (row) => <MoneyCell value={row.requestedAmount} /> }, { key: "monthlyInstallment", label: "Installment", render: (row) => <MoneyCell value={row.monthlyInstallment} /> }, { key: "outstandingBalance", label: "Outstanding", render: (row) => <MoneyCell value={row.outstandingBalance} /> }, { key: "loanStatus", label: "Loan status", render: (row) => <StatusBadge value={row.loanStatus} /> }, { key: "actions", label: "Actions", render: (row) => <div className="flex gap-1">{["ACTIVE", "COMPLETED"].includes(row.loanStatus) && <Button size="sm" variant="ghost" title="Schedule" onClick={() => onSchedule(row)}><ListOrdered size={15} /></Button>}{canPrepare && ["DRAFT", "REJECTED"].includes(row.workflowStatus) && <Button size="sm" variant="secondary" onClick={() => onSubmit(row.id)}><Send size={14} />Submit</Button>}{canPrepare && row.loanStatus === "APPROVED" && <Button size="sm" variant="success" onClick={() => onDisburse(row)}>Disburse</Button>}</div> },
  ]} />;
}

function RepaymentTable({ rows, canPrepare, onSubmit }) {
  return <DataTable rows={rows} columns={[{ key: "paymentDate", label: "Date", render: (row) => date(row.paymentDate) }, { key: "reference", label: "Reference", className: "font-semibold" }, { key: "memberName", label: "Member" }, { key: "loanNumber", label: "Loan" }, { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> }, { key: "createdBy", label: "Recorded by" }, { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> }, ...(canPrepare ? [{ key: "actions", label: "Actions", render: (row) => !row.batchId && ["DRAFT", "REJECTED"].includes(row.status) ? <Button size="sm" variant="secondary" onClick={() => onSubmit(row.id)}><Send size={14} />Submit</Button> : null }] : [])]} />;
}

function ScheduleModal({ schedule, onClose }) {
  return <Modal open={Boolean(schedule)} onClose={onClose} title="Repayment schedule" description={schedule ? `${schedule.loan.applicationNumber} · ${schedule.loan.memberName}` : ""} size="xl">{schedule && <><div className="mb-4 grid gap-3 sm:grid-cols-3"><Card className="bg-slate-50 shadow-none"><p className="text-xs text-slate-500">Total payable</p><p className="mt-1 font-bold">{money(schedule.loan.totalPayable)}</p></Card><Card className="bg-slate-50 shadow-none"><p className="text-xs text-slate-500">Monthly installment</p><p className="mt-1 font-bold">{money(schedule.loan.monthlyInstallment)}</p></Card><Card className="bg-slate-50 shadow-none"><p className="text-xs text-slate-500">Outstanding</p><p className="mt-1 font-bold">{money(schedule.loan.outstandingBalance)}</p></Card></div><DataTable pageSize={12} rows={schedule.rows} columns={[{ key: "installmentNumber", label: "#" }, { key: "dueDate", label: "Due date", render: (row) => date(row.dueDate) }, { key: "expectedAmount", label: "Expected", render: (row) => <MoneyCell value={row.expectedAmount} /> }, { key: "amountPaid", label: "Paid", render: (row) => <MoneyCell value={row.amountPaid} /> }, { key: "remainingAmount", label: "Remaining", render: (row) => <MoneyCell value={row.remainingAmount} /> }, { key: "paymentStatus", label: "Status", render: (row) => <StatusBadge value={row.paymentStatus} /> }]} /></>}</Modal>;
}

