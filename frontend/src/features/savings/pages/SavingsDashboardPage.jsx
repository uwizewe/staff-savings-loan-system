import { useMemo, useState } from 'react';
import { useAuth } from '../../auth/hooks/useAuth.jsx';
import { useApiData } from '../../../hooks/useApiData.js';
import { post } from '../../../services/api.js';
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, Modal, MoneyCell, PageHeader, StatusBadge, textareaClass } from '../../../components/ui/index.jsx';
import { date, money, routeTo, today } from '../../../utils/index.js';

export default function SavingsDashboardPage({ route }) {
  const { user } = useAuth();
  const id = new URLSearchParams(route.split('?')[1]).get('id') || user.memberId;
  const statement = useApiData(id ? `/members/${id}/statement` : '/savings');
  const [open, setOpen] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState('');
  const [form, setForm] = useState({ amount: '', type: 'DEPOSIT', comment: '' });
  const [draftId, setDraftId] = useState(null);
  const rows = useMemo(() => {
    let balance = 0;
    return [...(statement.data?.savings || [])].sort((a, b) => a.transactionDate.localeCompare(b.transactionDate) || a.id - b.id).map(item => {
      const signed = Math.round(Number(item.amount) * 100) * (item.savingType === 'WITHDRAWAL' ? -1 : 1);
      if (item.status === 'APPROVED') balance += signed;
      const monthlyComment = `${new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric' }).format(new Date(`${item.transactionDate}T12:00:00`))} Monthly Saving`;
      return { ...item, transaction: item.savingType === 'WITHDRAWAL' ? 'Withdraw' : item.savingType === 'MONTHLY' ? 'Monthly Saving' : 'Deposit', comment: item.savingType === 'MONTHLY' && (!item.description || item.description.startsWith('Monthly savings for')) ? monthlyComment : item.description || '—', signedAmount: signed / 100, balance: balance / 100 };
    }).reverse();
  }, [statement.data]);
  if (!id) return <Card>No member is linked to this account.</Card>;
  if (statement.loading) return <Loading label="Loading savings dashboard…"/>;
  if (statement.error) return <ErrorState message={statement.error} onRetry={statement.reload}/>;
  const data = statement.data;
  const canPrepare = ['INITIATOR', 'ADMIN'].includes(user.role);
  const save = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      if (!form.comment.trim()) throw new Error('Comment is required');
      if (form.type === 'WITHDRAWAL' && Number(form.amount) > Number(data.totalSavings)) throw new Error('Withdrawal exceeds available savings');
      let idToSubmit = draftId;
      if (!idToSubmit) {
        const created = await post(form.type === 'WITHDRAWAL' ? '/savings/withdrawals' : '/savings/individual', { memberId: Number(id), amount: Number(form.amount), transactionDate: today(), description: form.comment.trim() });
        idToSubmit = created.id; setDraftId(created.id);
      }
      await post(`/savings/${idToSubmit}/submit`);
      setOpen(false); setDraftId(null); statement.reload();
    } catch (failure) { setError(failure.message); } finally { setBusy(false); }
  };
  return <div className="min-w-0 space-y-6">
    {user.role !== 'MEMBER' && <Button variant="ghost" onClick={() => routeTo('savings?view=members')}>← Members Saving</Button>}
    <PageHeader title="Savings Dashboard" description={`${data.member.memberCode} · ${data.member.fullName}`} actions={canPrepare && <Button onClick={() => { setForm({ amount: '', type: 'DEPOSIT', comment: '' }); setDraftId(null); setError(''); setOpen(true); }}>Member Savings Transaction</Button>}/>
    <div className="grid gap-4 sm:grid-cols-2"><Card className="!bg-teal-800 text-white"><p className="text-sm text-teal-100">Total Saving</p><p className="mt-3 break-words text-3xl font-semibold">{money(data.totalSavings)}</p><p className="mt-3 text-xs text-teal-100">Available approved savings balance</p></Card><Card><p className="text-sm text-slate-500">Monthly Saving</p><p className="mt-3 text-3xl font-semibold text-slate-900">{money(data.member.monthlySavingAmount)}</p><p className="mt-3 text-xs text-slate-500">Regular monthly contribution</p></Card></div>
    <section className="min-w-0"><h2 className="mb-3 text-lg font-semibold">Savings Statement</h2><p className="mb-4 text-sm text-slate-500">Balances reflect approved savings transactions. Pending transactions do not change your balance.</p><DataTable label="Savings statement" rows={rows} columns={[
      { key: 'transactionDate', label: 'Date', render: row => date(row.transactionDate) },
      { key: 'transaction', label: 'Transaction' }, { key: 'comment', label: 'Comment', className: 'min-w-48 max-w-sm !whitespace-normal' },
      { key: 'signedAmount', label: 'Amount', render: row => <MoneyCell value={row.signedAmount}/> },
      { key: 'balance', label: 'Balance', render: row => <MoneyCell value={row.balance}/> },
      { key: 'status', label: 'Status', render: row => <StatusBadge value={row.status}/> },
    ]}/></section>
    <Modal open={open} onClose={() => { if (!busy) { setOpen(false); statement.reload(); } }} title="Member Savings Transaction" description={data.member.fullName}>
      <form className="space-y-4" onSubmit={save}>{error && <ErrorState message={error}/>}<p className="rounded-xl bg-teal-50 p-3 text-sm text-teal-800">Available savings: {money(data.totalSavings)}</p>
        <fieldset disabled={busy || Boolean(draftId)} className="space-y-4"><Field label="Amount" required><input type="number" min="0.01" max={form.type === 'WITHDRAWAL' ? data.totalSavings : undefined} step="0.01" required className={inputClass} value={form.amount} onChange={event => setForm({ ...form, amount: event.target.value })}/></Field>
        <Field label="Transaction Type" required><select className={inputClass} value={form.type} onChange={event => setForm({ ...form, type: event.target.value })}><option value="DEPOSIT">Deposit</option><option value="WITHDRAWAL">Withdraw</option></select></Field>
        <Field label="Comment" required><textarea required maxLength={500} className={textareaClass} value={form.comment} onChange={event => setForm({ ...form, comment: event.target.value })}/></Field></fieldset>
        <p className="text-xs text-slate-500">The balance changes after independent approval.</p><div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="secondary" disabled={busy} onClick={() => { setOpen(false); statement.reload(); }}>Cancel</Button><Button type="submit" loading={busy}>{draftId ? 'Retry submission' : 'Submit for approval'}</Button></div>
      </form>
    </Modal>
  </div>;
}

