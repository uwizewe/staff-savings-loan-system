import ApprovalActions from '../components/ApprovalActions.jsx';
import { Card, DataTable, ErrorState, Loading, MoneyCell, PageHeader } from '../../../components/ui/index.jsx';
import { useApiData } from '../../../hooks/useApiData.js';
import { dateTime } from '../../../utils/index.js';

export default function ApprovalsPage() {
  const { data, loading, error, reload } = useApiData('/approvals');
  if (loading) return <Loading label="Loading approvals…" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  return <><PageHeader title="Pending approvals" description="Review submitted financial transactions and staff payment details." />{!data.length ? <Card><h2 className="font-bold">Everything is up to date</h2><p className="text-sm text-slate-500">There are no transactions waiting for approval.</p></Card> : <DataTable rows={data} keyField={row => row.type + '-' + row.id} columns={[
    { key: 'type', label: 'Type', render: row => row.type.replaceAll('_', ' ') },
    { key: 'reference', label: 'Reference' }, { key: 'description', label: 'Transaction' },
    { key: 'amount', label: 'Amount', render: row => <MoneyCell value={row.amount} /> },
    { key: 'createdBy', label: 'Prepared by' }, { key: 'submittedAt', label: 'Submitted', render: row => dateTime(row.submittedAt) },
    { key: 'actions', label: 'Decision', render: row => <div className="flex gap-2"><ApprovalActions type={row.type} record={{ ...row, status: 'PENDING_APPROVAL' }} onDone={reload} batchItemsPath={row.type === 'REPAYMENT_BATCH' ? '/loans/repayment-batches/' + row.id + '/items' : row.type === 'SAVINGS_BATCH' ? '/savings/batches/' + row.id + '/items' : undefined} /></div> }
  ]} />}</>;
}
