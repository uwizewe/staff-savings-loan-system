import { useState } from 'react';
import { get, post } from '../../../services/api.js';
import { Button, ErrorState, Field, inputClass, Modal, MoneyCell, textareaClass } from '../../../components/ui/index.jsx';
import { today, date, money } from '../../../utils/index.js';
import LoanBreakdown from './LoanBreakdown.jsx';

const labels = { TOP_UP: 'Top Up', RESCHEDULE: 'Reschedule', RESTRUCTURE: 'Restructure', INSTALLMENT: 'Installment Payment', FULL_SETTLEMENT: 'Full Settlement' };
export default function LoanActionModal({ type, loan, rows, onClose, onDone }) {
  const installment = rows.find(r => Number(r.remainingAmount) > 0);
  const payment = ['INSTALLMENT', 'FULL_SETTLEMENT'].includes(type);
  const [form, setForm] = useState({ type, amount: type === 'INSTALLMENT' ? installment?.remainingAmount ?? '' : 0, months: loan.installmentsRemaining, effectiveDate: today(), paymentDate: today(), remarks: '' });
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const change = values => { setForm({ ...form, ...values }); setPreview(null); };
  const calculate = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const result = type === 'FULL_SETTLEMENT' ? await get(`/loans/${loan.id}/settlement-quote`) : await post(`/loans/${loan.id}/adjustment-preview`, form);
      setPreview({ ...result, source: JSON.stringify(form) });
    } catch(e) { setError(e.message); } finally { setBusy(false); }
  };
  const submit = async event => {
    event?.preventDefault(); setBusy(true); setError('');
    try {
      if (payment) {
        const record = await post('/loans/repayments', { loanId: loan.id, scheduleId: type === 'INSTALLMENT' ? installment?.id : null, paymentType: type, amount: type === 'FULL_SETTLEMENT' ? preview.settlementAmount : form.amount, paymentDate: form.paymentDate, remarks: form.remarks });
        await post(`/loans/repayments/${record.id}/submit`);
      } else await post(`/loans/${loan.id}/adjustments`, form);
      onDone(); onClose();
    } catch(e) { setError(e.message); } finally { setBusy(false); }
  };
  const validPreview = preview && preview.source === JSON.stringify(form);
  return <Modal open onClose={() => !busy && onClose()} title={labels[type]} description={`${loan.applicationNumber} · ${loan.memberName}`} size={payment ? 'lg' : '2xl'}><form onSubmit={type === 'INSTALLMENT' ? submit : calculate} className="space-y-5">
    {error && <ErrorState message={error} />}
    <div className="grid grid-cols-2 gap-3 rounded-2xl bg-slate-50 p-4">{[['Remaining principal', money(loan.remainingPrincipal)], ['Annual interest', `${loan.annualInterestRate}%`], ['Remaining term', `${loan.installmentsRemaining} installments`], ['Current installment', money(loan.monthlyInstallment)]].map(([label,value]) => <div key={label}><p className="text-xs text-slate-500">{label}</p><p className="font-semibold">{value}</p></div>)}</div>
    {type === 'INSTALLMENT' && installment && <div className="rounded-xl border p-4 text-sm"><p className="font-bold">Installment #{installment.installmentNumber} · Due {date(installment.dueDate)}</p><p>Expected principal: {money(installment.principalAmount)} · Interest: {money(installment.interestAmount)}</p><p>Expected installment: {money(installment.expectedAmount)} · Still due: {money(installment.remainingAmount)}</p><p>Outstanding loan: {money(loan.outstandingBalance)}</p></div>}
    <fieldset disabled={busy} className="grid gap-4 sm:grid-cols-2">
      {!['RESCHEDULE', 'FULL_SETTLEMENT'].includes(type) && <Field label={payment ? 'Amount paid' : 'Additional principal'} required><input className={inputClass} required type="number" min="0.01" max={type === 'INSTALLMENT' ? installment?.remainingAmount : undefined} step="0.01" value={form.amount} onChange={e => change({ amount: e.target.value })} /></Field>}
      {!payment && <Field label="New / applicable term" required><input className={inputClass} required type="number" min="1" max="120" value={form.months} onChange={e => change({ months: e.target.value })} /></Field>}
      <Field label={payment ? 'Payment date' : 'Effective date'} required><input className={inputClass} type="date" min={loan.disbursementDate} max={today()} required value={payment ? form.paymentDate : form.effectiveDate} onChange={e => change({ [payment ? 'paymentDate' : 'effectiveDate']: e.target.value })} /></Field>
      <Field label="Description / remarks" required className="sm:col-span-2"><textarea className={textareaClass} maxLength={255} required value={form.remarks} onChange={e => change({ remarks: e.target.value })} /></Field>
    </fieldset>
    {type === 'FULL_SETTLEMENT' && <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Settlement includes the remaining principal plus one monthly term of interest. Future interest is waived only when the payment is approved.</p>}
    {type !== 'INSTALLMENT' && <Button type="submit" loading={busy}>{payment ? 'Calculate settlement quote' : 'Preview new schedule'}</Button>}
    {validPreview && (type === 'FULL_SETTLEMENT' ? <div className="grid grid-cols-2 gap-3 rounded-xl bg-teal-50 p-4">{[['Remaining principal',preview.remainingPrincipal],['One month interest',preview.applicableInterest],['Charges',preview.charges],['Total settlement',preview.settlementAmount],['Interest waived',preview.interestWaived]].map(([label,value]) => <div key={label}><p className="text-xs text-teal-700">{label}</p><p className="font-bold"><MoneyCell value={value} /></p></div>)}</div> : <LoanBreakdown value={preview} />)}
    <div className="flex flex-wrap justify-end gap-2 border-t pt-4"><Button type="button" variant="secondary" disabled={busy} onClick={onClose}>Cancel</Button>{type === 'INSTALLMENT' ? <Button type="submit" loading={busy} disabled={!installment}>Submit payment</Button> : <Button type="button" disabled={busy || !validPreview} onClick={submit}>Submit for approval</Button>}</div>
  </form></Modal>;
}
