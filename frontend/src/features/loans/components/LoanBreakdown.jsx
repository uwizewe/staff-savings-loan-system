import { DataTable, MoneyCell } from '../../../components/ui/index.jsx';
import { money } from '../../../utils/index.js';

export default function LoanBreakdown({ value }) {
  if (!value) return null;
  return <div className="space-y-4"><p className="text-sm text-slate-600">Reducing balance · {value.annualRate}% annual ({Number(value.annualRate / 12).toFixed(4)}% monthly) · {value.months} monthly installments. Final installment adjusts for rounding.</p><div className="grid grid-cols-2 gap-3 sm:grid-cols-4">{[['Principal', value.principal], ['Total interest', value.interest], ['Regular installment', value.monthlyInstallment], ['Total payable', value.totalPayable]].map(([label, amount]) => <div key={label} className="rounded-xl bg-teal-50 p-3"><p className="text-xs text-teal-700">{label}</p><p className="font-bold">{money(amount)}</p></div>)}</div><DataTable rows={value.rows} keyField="number" pageSize={10} columns={[{ key: 'number', label: '#' }, { key: 'dueDate', label: 'Due date' }, { key: 'paymentStatus', label: 'Status', render: row => row.paymentStatus || 'PENDING' }, ...[['openingBalance', 'Opening principal'], ['principal', 'Principal'], ['interest', 'Interest'], ['amount', 'Installment'], ['closingBalance', 'Closing principal']].map(([key, label]) => ({ key, label, render: row => <MoneyCell value={row[key]} /> }))]} /></div>;
}
