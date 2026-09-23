import { Button, Card, DataTable, Modal, MoneyCell, StatusBadge } from '../../../components/ui/index.jsx';
import { date, money } from '../../../utils/index.js';
import { Send, ListOrdered } from 'lucide-react';
import ApprovalActions from '../../approvals/components/ApprovalActions.jsx';

export default function SavingsTable({ rows, canSubmit, onSubmit, canApprove, batches = [], onDecision }) {
  return <DataTable rows={rows} columns={[
    { key: "transactionDate", label: "Date", render: (row) => date(row.transactionDate) },
    { key: "reference", label: "Reference", className: "font-semibold text-teal-800" },
    { key: "memberName", label: "Member" },
    { key: "savingType", label: "Type" },
    { key: "amount", label: "Amount", render: (row) => <MoneyCell value={row.amount} /> },
    { key: "createdBy", label: "Recorded by" },
    { key: "status", label: "Status", render: (row) => <StatusBadge value={row.status} /> },
        ...(canSubmit || canApprove ? [{ key: "actions", label: "Actions", render: (row) => <div className="flex gap-2">{canSubmit && !row.batchId && ["DRAFT", "REJECTED"].includes(row.status) && <Button size="sm" variant="secondary" onClick={() => onSubmit(row.id)}><Send size={14} />Submit</Button>}{canApprove && row.status === "PENDING_APPROVAL" && <ApprovalActions type={row.batchId ? "SAVINGS_BATCH" : "SAVING"} record={row.batchId ? batches.find((batch) => batch.id === row.batchId) : row} batchItemsPath={row.batchId ? `/savings/batches/${row.batchId}/items` : undefined} onDone={onDecision} />}</div> }] : []),
  ]} />;
}
