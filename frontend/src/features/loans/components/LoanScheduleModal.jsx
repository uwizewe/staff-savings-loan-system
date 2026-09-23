import { useState } from 'react';
import { Modal, inputClass } from '../../../components/ui/index.jsx';
import { date, dateTime } from '../../../utils/index.js';
import InstallmentTable from './InstallmentTable.jsx';

export default function LoanScheduleModal({ open, onClose, loan, rows, history }) {
  const [version,setVersion]=useState('');
  const selected=history.find(item=>String(item.id)===version);
  return (<Modal open={open} onClose={onClose} title="Installment schedule" description={`${loan.applicationNumber} · ${loan.memberName}`} size="2xl"><section className="space-y-4"><div className="flex flex-wrap items-center justify-between gap-3"><h3 className="font-bold">Installment schedule</h3><label className="text-xs text-slate-500">View schedule history<select className={inputClass} value={version} onChange={e=>setVersion(e.target.value)}><option value="">Current active schedule</option>{history.map(v=><option key={v.id} value={v.id}>Version {v.versionNumber} · {v.scheduleType.replaceAll('_',' ')} · {v.active?'Active':v.status}</option>)}</select></label></div>
        {selected && <div className="rounded-xl bg-slate-50 p-3 text-sm"><p>Version {selected.versionNumber} · Effective {date(selected.effectiveDate)} · {selected.active?'Active':'Read-only history'}</p><p>Initiated by {selected.createdBy} · {dateTime(selected.createdAt)}</p><p>{selected.status==='REJECTED'?'Rejected':'Approved'} by {selected.actionedBy || 'Awaiting decision'} · {dateTime(selected.actionedAt)}</p><p>Principal {selected.previousPrincipal} + {selected.additionalAmount} → {selected.principal} · Term {selected.previousTerm} → {selected.term}</p><p>{selected.remarks}</p>{selected.decisionRemarks && <p>Decision: {selected.decisionRemarks}</p>}</div>}
        <InstallmentTable rows={selected?.rows || rows} historical={Boolean(selected && !selected.active && selected.status==='APPROVED')} />
      </section></Modal>);
}
