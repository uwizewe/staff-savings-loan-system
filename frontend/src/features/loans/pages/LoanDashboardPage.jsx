import { useEffect, useState } from 'react';
import { get } from '../../../services/api.js';
import { Button, ErrorState, Loading, MoneyCell, PageHeader, StatusBadge } from '../../../components/ui/index.jsx';
import { date, routeTo } from '../../../utils/index.js';
import LoanScheduleModal from '../components/LoanScheduleModal.jsx';
import { useAuth } from '../../auth/hooks/useAuth.jsx';
import LoanActionModal from '../components/LoanActionModal.jsx';
import RepaymentTable from '../../repayments/components/RepaymentTable.jsx';
import ApprovalActions from '../../approvals/components/ApprovalActions.jsx';

export default function LoanDashboardPage({ route }) {
  const {user}=useAuth();
  const params=new URLSearchParams(route.split('?')[1]||'');
  const id=params.get('id');
  const origin=['applications','active','closed'].includes(params.get('from'))?params.get('from'):'active';
  const [scheduleOpen,setScheduleOpen]=useState(false);
  const [data, setData] = useState(null), [error,setError] = useState(''), [loading,setLoading] = useState(true);
  const [action,setAction] = useState(null), [revision,setRevision] = useState(0);
  useEffect(() => { let live = true; setLoading(true); setError(''); Promise.all([get(`/loans/${id}`), get(`/loans/${id}/schedule`), get(`/loans/${id}/schedule-history`), get(`/loans/repayments?loanId=${id}`)]).then(([loan, rows, history, payments]) => { if(live) setData({loan,rows,history,payments}); }).catch(e => {if(live) setError(e.message);}).finally(() => {if(live) setLoading(false);}); return () => {live=false;}; }, [id,revision]);
  const refresh = () => { setRevision(r=>r+1);  };
  const loan=data?.loan;
  const canPrepare=['INITIATOR','ADMIN'].includes(user.role);
  const total=(loan?.installmentsPaid || 0)+(loan?.installmentsRemaining || 0);
  const progress=total ? Math.round(100*loan.installmentsPaid/total) : 0;
  return <div className="min-w-0 space-y-6"><Button variant="ghost" onClick={()=>routeTo(`loans?view=${origin}${params.get('memberId')?`&memberId=${params.get('memberId')}`:''}`)}>← Back to {origin==='closed'?'closed loans':origin==='active'?'active loans':'loan applications'}</Button><PageHeader title={loan && ['CLOSED','COMPLETED'].includes(loan.loanStatus)?'Closed Loan Dashboard':'Loan Dashboard'} description={loan ? `${loan.applicationNumber} · ${loan.memberName}` : 'Loading loan'} actions={loan&&<><Button variant="ghost" onClick={()=>routeTo(`statement?id=${loan.memberId}`)}>Loan Statement</Button><Button variant="secondary" onClick={()=>setScheduleOpen(true)}>View Installment Schedule</Button></>}/>
    {loading ? <Loading /> : error ? <ErrorState message={error} onRetry={()=>setRevision(r=>r+1)} /> : data && <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-teal-100 bg-teal-50/50 p-5"><div><p className="text-lg font-bold">{loan.memberName}</p><p className="text-sm text-slate-500">{loan.memberCode} · {loan.categoryName || 'Legacy category'} · Start {date(loan.disbursementDate || loan.applicationDate)}</p><p className="text-sm text-slate-500">{loan.annualInterestRate}% annually · Original term {loan.originalTerm} · Remaining {loan.installmentsRemaining}</p></div><StatusBadge value={loan.loanStatus} /></div>
      <div className="grid grid-cols-1 gap-3 min-[380px]:grid-cols-2 xl:grid-cols-4">{[['Loan Amount','originalPrincipal'],['Current principal','currentPrincipal'],['Interest','totalInterest'],['Installment Amount','monthlyInstallment'],['Total Loan','totalPayable'],['Paid Principal','paidPrincipal'],['Paid Interest','paidInterest'],['Total paid','amountRepaid'],['Remaining principal','remainingPrincipal'],['Remaining Balance','outstandingBalance']].map(([label,key])=><div key={key} className="rounded-xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs text-slate-500">{label}</p><p className="mt-1 font-bold"><MoneyCell value={loan[key]} /></p></div>)}{[['Installments paid',loan.installmentsPaid],['Installments remaining',loan.installmentsRemaining]].map(([label,value])=><div key={label} className="rounded-xl border border-teal-100 bg-teal-50 p-3"><p className="text-xs text-teal-700">{label}</p><p className="mt-1 font-bold">{value}</p></div>)}</div>
      <div><div className="mb-2 flex justify-between text-sm font-semibold"><span>{loan.installmentsPaid} of {total} installments paid</span><span>{progress}%</span></div><div role="progressbar" aria-label="Installments repaid" aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress} className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full bg-teal-600" style={{width:`${progress}%`}} /></div></div>
      {loan.adjustmentType ? <div className="flex flex-wrap items-center gap-3 rounded-xl bg-amber-50 p-4"><span className="text-sm">{loan.adjustmentType.replaceAll('_',' ')} awaiting approval</span><ApprovalActions type="LOAN_CHANGE" record={{ ...loan, status:'PENDING_APPROVAL', workflowStatus:'PENDING_APPROVAL', amount:loan.adjustmentAmount }} onDone={refresh} /></div> : canPrepare && loan.loanStatus==='ACTIVE' && <div className="flex flex-wrap gap-2">{[['TOP_UP','Top Up'],['RESCHEDULE','Reschedule'],['RESTRUCTURE','Restructure'],['INSTALLMENT','Installment Payment'],['FULL_SETTLEMENT','Full Settlement']].map(([type,label])=><Button key={type} variant={type==='FULL_SETTLEMENT'?'secondary':'primary'} onClick={()=>setAction(type)}>{label}</Button>)}</div>}
      <LoanScheduleModal open={scheduleOpen} onClose={()=>setScheduleOpen(false)} loan={loan} rows={data.rows} history={data.history}/>

      <section className="min-w-0"><h3 className="mb-3 font-bold">Repayment history</h3><RepaymentTable rows={data.payments} /></section>
    </div>}
  {action && loan && <LoanActionModal type={action} loan={loan} rows={data.rows} onClose={()=>setAction(null)} onDone={refresh} />}</div>;
}
