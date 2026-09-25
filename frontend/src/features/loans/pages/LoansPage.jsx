import { useState } from 'react';
import { get,post } from '../../../services/api.js';
import { useAuth } from '../../auth/hooks/useAuth.jsx';
import { useApiData } from '../../../hooks/useApiData.js';
import { Button, DataTable, ErrorState, Loading, MoneyCell, PageHeader, StatusBadge } from '../../../components/ui/index.jsx';
import { date,routeTo } from '../../../utils/index.js';
import ApprovalActions from '../../approvals/components/ApprovalActions.jsx';
import LoanCreationModal from '../components/LoanCreationModal.jsx';
import RepaymentTable from '../../repayments/components/RepaymentTable.jsx';
import MonthlyRepayments from '../../repayments/components/MonthlyRepayments.jsx';
export default function LoansPage({route}) {
  const {user}=useAuth();
  const loans=useApiData('/loans'),payments=useApiData('/loans/repayments');
  const [creation,setCreation]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const params=new URLSearchParams(route.split('?')[1]||'');
  const view=params.get('view')||params.get('tab')||'applications';
  const pageTitle={applications:'Loan applications',active:'Active loans',repayments:'Repayment history',monthly:'Monthly Loan',closed:'Closed Loan'}[view]||'Loan applications';
  const canPrepare=['INITIATOR','ADMIN'].includes(user.role);
  const refresh=()=>{loans.reload();payments.reload();};
  const submitLoan=async id=>{setBusy(true);setError('');try{await post(`/loans/${id}/submit`);refresh();}catch(e){setError(e.message);}finally{setBusy(false);}};
  const submitPayment=async id=>{setBusy(true);setError('');try{await post(`/loans/repayments/${id}/submit`);refresh();}catch(e){setError(e.message);}finally{setBusy(false);}};
  const create=async()=>{setBusy(true);setError('');try{setCreation(await get('/members'));}catch(e){setError(e.message);}finally{setBusy(false);}};
  const memberId=params.get('memberId');
  const rows=(loans.data||[]).filter(l=>(!memberId||String(l.memberId)===memberId)&&(view==='active'?l.loanStatus==='ACTIVE':view==='closed'?['CLOSED','COMPLETED'].includes(l.loanStatus):true));
  const moneyColumn=(key,label)=>({key,label,render:r=><MoneyCell value={r[key]}/>});
  const actions={key:'actions',label:'Actions',render:r=><div className="flex gap-2"><Button size="sm" variant="secondary" onClick={()=>routeTo(`loans?view=dashboard&id=${r.id}&from=${view}${memberId?`&memberId=${memberId}`:''}`)}>View dashboard</Button>{canPrepare&&['DRAFT','REJECTED'].includes(r.loanStatus)&&<Button size="sm" disabled={busy} onClick={()=>submitLoan(r.id)}>Submit</Button>}<ApprovalActions type="LOAN" record={r} onDone={refresh}/></div>};
  return <><PageHeader title={pageTitle} description="Manage staff lending with clear balances, schedules and approval history." actions={canPrepare&&<Button loading={busy} onClick={create}>New loan application</Button>}/>
    {error&&<ErrorState message={error}/>}
    {view==='monthly'&&user.role!=='MEMBER'?<MonthlyRepayments canPrepare={canPrepare} onDone={refresh}/>:loans.loading||payments.loading?<Loading/>:loans.error||payments.error?<ErrorState message={loans.error||payments.error} onRetry={refresh}/>:view==='repayments'?<RepaymentTable rows={payments.data} canPrepare={canPrepare} onSubmit={submitPayment} onDone={refresh}/>:<><DataTable key={view} label={pageTitle} searchPlaceholder="Search staff, loan or category…" rows={rows} columns={['active','closed'].includes(view)?[
      {key:'applicationNumber',label:'Loan number'},{key:'memberCode',label:'Staff ID'},{key:'memberName',label:'Staff name'},{key:'categoryName',label:'Category'},
      moneyColumn('originalPrincipal','Original amount'),moneyColumn('currentPrincipal','Current principal'),{key:'annualInterestRate',label:'Annual rate',render:r=>`${r.annualInterestRate}%`},moneyColumn('monthlyInstallment','Installment'),moneyColumn('amountRepaid','Paid'),moneyColumn('outstandingBalance','Outstanding'),{key:'installmentsRemaining',label:'Remaining installments'},{key:'disbursementDate',label:'Activation date',render:r=>date(r.disbursementDate)},{key:'loanStatus',label:'Status',render:r=><StatusBadge value={r.loanStatus}/>},actions
    ]:[{key:'applicationNumber',label:'Loan number'},{key:'memberName',label:'Staff name'},{key:'categoryName',label:'Category'},{key:'applicationDate',label:'Application date',render:r=>date(r.applicationDate)},moneyColumn('requestedAmount','Requested'),moneyColumn('monthlyInstallment','Installment'),{key:'loanStatus',label:'Status',render:r=><StatusBadge value={r.loanStatus}/>},actions]}/></>}
    {creation&&<LoanCreationModal members={creation} loans={loans.data||[]} onClose={()=>setCreation(null)} onDone={refresh}/>}
  </>;
}
