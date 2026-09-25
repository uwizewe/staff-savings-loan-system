import { useState } from 'react';
import { useApiData } from '../../../hooks/useApiData.js';
import { Button, DataTable, ErrorState, Modal, PageHeader, StatusBadge } from '../../../components/ui/index.jsx';

export default function RolesPermissionsPage() {
  const permissions = useApiData('/admin/permissions');
  const [selected, setSelected] = useState(null);
  const rows = Object.entries(permissions.data || {}).map(([role, items]) => ({ role, permissions: items, count: items.length }));
  return <div className="space-y-5">
    <PageHeader title="Roles & Permissions" description="System Settings · Review the access assigned to each built-in role."/>
    <p className="rounded-xl border border-teal-100 bg-teal-50 p-4 text-sm text-teal-900">Roles are managed by the system. Assign a role to an account from the Users page.</p>
    {permissions.error ? <ErrorState message={permissions.error} onRetry={permissions.reload}/> : <DataTable label="Roles and permissions" rows={rows} keyField="role" loading={permissions.loading} columns={[
      { key: 'role', label: 'Role', render: row => <StatusBadge value={row.role}/> },
      { key: 'count', label: 'Permissions' },
      { key: 'permissions', label: 'Access summary', className: 'min-w-60 max-w-xl !whitespace-normal', render: row => row.permissions.join(' · ') },
      { key: 'actions', label: 'Actions', render: row => <Button size="sm" variant="secondary" onClick={() => setSelected(row)}>View permissions</Button> },
    ]}/>}
    <Modal open={Boolean(selected)} onClose={() => setSelected(null)} title="Role permissions" description={selected?.role}>
      <ul className="space-y-3">{selected?.permissions.map(permission => <li key={permission} className="rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-700">{permission}</li>)}</ul>
    </Modal>
  </div>;
}
