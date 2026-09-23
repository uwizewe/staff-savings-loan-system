import { useState } from 'react';
import { post } from '../../../services/api.js';
import { useApiData } from '../../../hooks/useApiData.js';
import { Button, ErrorState, Field, inputClass, Loading, Modal, textareaClass } from '../../../components/ui/index.jsx';
import { today } from '../../../utils/index.js';
import LoanBreakdown from './LoanBreakdown.jsx';

export default function LoanCreationModal({ members, loans, onClose, onDone }) {
  const categories = useApiData('/loan-categories?activeOnly=true');
  const [form, setForm] = useState({ memberId: '', categoryId: '', applicationDate: today(), firstInstallmentDate: '', requestedAmount: '', annualInterestRate: '', repaymentMonths: 12, purpose: '', remarks: '' });
  const [preview, setPreview] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const change = values => { setForm({ ...form, ...values }); setPreview(null); };
  const calculate = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try { const data = await post(`/loans/preview?categoryId=${form.categoryId}`, form); setPreview({ ...data, source: JSON.stringify(form) }); }
    catch(e) { setError(e.message); } finally { setBusy(false); }
  };
  const save = async submit => {
    if (!preview || preview.source !== JSON.stringify(form)) return;
    setBusy(true); setError('');
    try { const loan = await post(`/loans?categoryId=${form.categoryId}`, { ...form, annualInterestRate: preview.annualRate }); if (submit) await post(`/loans/${loan.id}/submit`); onDone(); onClose(); }
    catch(e) { setError(e.message); } finally { setBusy(false); }
  };
  return <Modal open onClose={() => !busy && onClose()} title="New loan application" description="Set the loan assumptions, then review the complete schedule before saving." size="2xl">
    {categories.loading ? <Loading /> : categories.error ? <ErrorState message={categories.error} onRetry={categories.reload} /> : <form onSubmit={calculate} className="space-y-5">
      {error && <ErrorState message={error} />}
      <fieldset disabled={busy} className="grid gap-4 sm:grid-cols-3">
        <Field label="Staff / member" required><select className={inputClass} required value={form.memberId} onChange={e => change({ memberId: e.target.value })}><option value="">Select staff member</option>{members.filter(m => m.membershipStatus === 'ACTIVE' && !loans.some(l => l.memberId === m.id && ['ACTIVE', 'APPROVED', 'PENDING_APPROVAL'].includes(l.loanStatus))).map(m => <option key={m.id} value={m.id}>{m.memberCode} — {m.fullName}</option>)}</select></Field>
        <Field label="Principal amount" required><input className={inputClass} type="number" min="1" step="0.01" required value={form.requestedAmount} onChange={e => change({ requestedAmount: e.target.value })} /></Field>
        <Field label="Loan category" required><select className={inputClass} required value={form.categoryId} onChange={e => { const c = categories.data.find(c => c.id === Number(e.target.value)); change({ categoryId: e.target.value, annualInterestRate: c?.annualRate ?? '' }); }}><option value="">Select active category</option>{categories.data.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></Field>
        <Field label="Annual interest (%)"><input className={`${inputClass} bg-slate-50`} readOnly value={form.annualInterestRate} /></Field>
        <Field label="Term (monthly installments)" required><input className={inputClass} type="number" min="1" max="120" required value={form.repaymentMonths} onChange={e => change({ repaymentMonths: e.target.value })} /></Field>
        <Field label="First installment date" required><input className={inputClass} type="date" min={form.applicationDate} required value={form.firstInstallmentDate} onChange={e => change({ firstInstallmentDate: e.target.value })} /></Field>
        <Field label="Application date" required><input className={inputClass} type="date" required value={form.applicationDate} onChange={e => change({ applicationDate: e.target.value })} /></Field>
        <Field label="Purpose" required><input className={inputClass} maxLength={500} required value={form.purpose} onChange={e => change({ purpose: e.target.value })} /></Field>
        <Field label="Remarks"><textarea className={textareaClass} maxLength={1000} value={form.remarks} onChange={e => change({ remarks: e.target.value })} /></Field>
      </fieldset>
      {!categories.data.length && <p className="text-sm text-amber-700">An administrator must create an active category in System Settings → Loan Categories.</p>}
      <Button type="submit" loading={busy}>Calculate and preview schedule</Button>
      <LoanBreakdown value={preview?.source === JSON.stringify(form) ? preview : null} />
      <div className="flex flex-wrap justify-end gap-2 border-t pt-4"><Button type="button" variant="secondary" disabled={busy} onClick={onClose}>Cancel</Button><Button type="button" variant="secondary" disabled={busy || !preview} onClick={() => save(false)}>Save draft</Button><Button type="button" disabled={busy || !preview} onClick={() => save(true)}>Submit for approval</Button></div>
    </form>}
  </Modal>;
}
