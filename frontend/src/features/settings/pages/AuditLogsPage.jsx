import { useState } from 'react';
import { useApiData } from '../../../hooks/useApiData.js';
import { Button, DataTable, ErrorState, Modal, PageHeader } from '../../../components/ui/index.jsx';
import { dateTime } from '../../../utils/index.js';

export default function AuditLogsPage() {
  const audits = useApiData('/admin/audit-logs');
  const [selected, setSelected] = useState(null);
  return <div className="space-y-5">
    <PageHeader title="Audit Logs" description="System Settings · Read-only history of account and financial activity."/>
    {audits.error ? <ErrorState message={audits.error} onRetry={audits.reload}/> : <DataTable label="Audit logs" loading={audits.loading} rows={audits.data} pageSize={25} columns={[
      { key: 'createdAt', label: 'Date & time', render: row => dateTime(row.createdAt) },
      { key: 'user', label: 'User' }, { key: 'action', label: 'Action' },
      { key: 'entityType', label: 'Area' }, { key: 'reference', label: 'Reference' },
      { key: 'previousStatus', label: 'Previous status' }, { key: 'newStatus', label: 'New status' },
      { key: 'details', label: 'Details', className: 'max-w-64 truncate' },
      { key: 'actions', label: 'Actions', render: row => <Button size="sm" variant="secondary" onClick={() => setSelected(row)}>View details</Button> },
    ]}/>}
    <Modal open={Boolean(selected)} onClose={() => setSelected(null)} title="Audit entry" description={selected ? dateTime(selected.createdAt) : ''}>
      <dl className="grid gap-4 sm:grid-cols-2">{[['User','user'],['Action','action'],['Area','entityType'],['Reference','reference'],['Previous status','previousStatus'],['New status','newStatus']].map(([label,key]) => <div key={key}><dt className="text-xs font-semibold text-slate-500">{label}</dt><dd className="mt-1 break-words text-sm text-slate-900">{selected?.[key] || '—'}</dd></div>)}<div className="sm:col-span-2"><dt className="text-xs font-semibold text-slate-500">Details</dt><dd className="mt-2 whitespace-pre-wrap break-words rounded-xl bg-slate-50 p-4 text-sm">{selected?.details || 'No additional details'}</dd></div></dl>
    </Modal>
  </div>;
}
