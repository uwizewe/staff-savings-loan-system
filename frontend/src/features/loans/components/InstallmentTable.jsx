import { DataTable, MoneyCell, StatusBadge } from '../../../components/ui/index.jsx';
import { date } from '../../../utils/index.js';

export default function InstallmentTable({ rows, historical = false }) {
  return <DataTable rows={rows} pageSize={10} columns={[
    { key: 'installmentNumber', label: '#' }, { key: 'dueDate', label: 'Due date', render: r => date(r.dueDate) },
    ...[['openingBalance', 'Opening principal'], ['principalAmount', 'Principal'], ['interestAmount', 'Interest'], ['expectedAmount', 'Installment'], ['amountPaid', 'Paid'], ['closingBalance', 'Closing principal']].map(([key, label]) => ({ key, label, render: r => r[key] == null ? '—' : <MoneyCell value={r[key]} /> })),
    { key: 'paymentStatus', label: 'Status', render: r => <StatusBadge value={historical && Number(r.remainingAmount) > 0 ? 'REPLACED' : r.paymentStatus} /> },
  ]} />;
}
