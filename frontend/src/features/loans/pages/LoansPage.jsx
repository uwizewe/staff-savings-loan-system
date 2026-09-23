import { useState } from 'react';
import { get,post } from '../../../services/api.js';
import { useAuth } from '../../auth/hooks/useAuth.jsx';
import { useApiData } from '../../../hooks/useApiData.js';
import { Button, DataTable, ErrorState, Field, inputClass, Loading, Modal, MoneyCell, PageHeader, StatusBadge } from '../../../components/ui/index.jsx';
import { date,routeTo,today } from '../../../utils/index.js';
import ApprovalActions from '../../approvals/components/ApprovalActions.jsx';
import LoanCreationModal from '../components/LoanCreationModal.jsx';
import RepaymentTable from '../../repayments/components/RepaymentTable.jsx';
import MonthlyRepayments from '../../repayments/components/MonthlyRepayments.jsx';
export default function LoansPage({route}) {
  const {user}=useAuth();
  const loans=useApiData('/loans'),payments=useApiData('/loans/repayments');
  const [creation,setCreation]=useState(null),[disburse,setDisburse]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const [disburseForm,setDisburseForm]=useState({disbursementDate:today(),reference:''});
  const params=new URLSearchParams(route.split('?')[1]||'');
  const view=params.get('view')||params.get('tab')||'applications';
  const pageTitle={applications:'Loan applications',active:'Active loans',repayments:'Repayment history',monthly:'Monthly repayments'}[view]||'Loan applications';
  const canPrepare=['INITIATOR','ADMIN'].includes(user.role);
  const refresh=()=>{loans.reload();payments.reload();};
  const submitLoan=async id=>{setBusy(true);setError('');try{await post(`/loans/${id}/submit`);refresh();}catch(e){setError(e.message);}finally{setBusy(false);}};
  const submitPayment=async id=>{setBusy(true);setError('');try{await post(`/loans/repayments/${id}/submit`);refresh();}catch(e){setError(e.message);}finally{setBusy(false);}};
  const create=async()=>{setBusy(true);setError('');try{setCreation(await get('/members'));}catch(e){setError(e.message);}finally{setBusy(false);}};
  const saveDisbursement=async e=>{e.preventDefault();setBusy(true);setError('');try{await post(`/loans/${disburse.id}/disburse`,disburseForm);setDisburse(null);refresh();}catch(e){setError(e.message);}finally{setBusy(false);}};
  const rows=(loans.data||[]).filter(l=>view!=='active'||l.loanStatus==='ACTIVE');
  const moneyColumn=(key,label)=>({key,label,render:r=><MoneyCell value={r[key]}/>});
  const actions={key:'actions',label:'Actions',render:r=><div className="flex gap-2"><Button size="sm" variant="secondary" onClick={()=>routeTo(`loans?view=dashboard&id=${r.id}&from=${view}`)}>View dashboard</Button>{canPrepare&&['DRAFT','REJECTED'].includes(r.loanStatus)&&<Button size="sm" disabled={busy} onClick={()=>submitLoan(r.id)}>Submit</Button>}<ApprovalActions type="LOAN" record={r} onDone={refresh}/>{canPrepare&&r.loanStatus==='APPROVED'&&<Button size="sm" onClick={()=>{setDisburse(r);setDisburseForm({disbursementDate:today(),reference:''});}}>Disburse</Button>}</div>};
  return <><PageHeader title={pageTitle} description="Manage staff lending with clear balances, schedules and approval history." actions={canPrepare&&<Button loading={busy} onClick={create}>New loan application</Button>}/>
    {!disburse&&error&&<ErrorState message={error}/>}
    {view==='monthly'&&user.role!=='MEMBER'?<MonthlyRepayments canPrepare={canPrepare} onDone={refresh}/>:loans.loading||payments.loading?<Loading/>:loans.error||payments.error?<ErrorState message={loans.error||payments.error} onRetry={refresh}/>:view==='repayments'?<RepaymentTable rows={payments.data} canPrepare={canPrepare} onSubmit={submitPayment} onDone={refresh}/>:<><DataTable key={view} label={pageTitle} searchPlaceholder="Search staff, loan or category…" rows={rows} columns={view==='active'?[
      {key:'applicationNumber',label:'Loan number'},{key:'memberCode',label:'Staff ID'},{key:'memberName',label:'Staff name'},{key:'categoryName',label:'Category'},
      moneyColumn('originalPrincipal','Original amount'),moneyColumn('currentPrincipal','Current principal'),{key:'annualInterestRate',label:'Annual rate',render:r=>`${r.annualInterestRate}%`},moneyColumn('monthlyInstallment','Installment'),moneyColumn('amountRepaid','Paid'),moneyColumn('outstandingBalance','Outstanding'),{key:'installmentsRemaining',label:'Remaining installments'},{key:'disbursementDate',label:'Start date',render:r=>date(r.disbursementDate)},{key:'loanStatus',label:'Status',render:r=><StatusBadge value={r.loanStatus}/>},actions
    ]:[{key:'applicationNumber',label:'Loan number'},{key:'memberName',label:'Staff name'},{key:'categoryName',label:'Category'},{key:'applicationDate',label:'Application date',render:r=>date(r.applicationDate)},moneyColumn('requestedAmount','Requested'),moneyColumn('monthlyInstallment','Installment'),{key:'loanStatus',label:'Status',render:r=><StatusBadge value={r.loanStatus}/>},actions]}/></>}
    {creation&&<LoanCreationModal members={creation} loans={loans.data||[]} onClose={()=>setCreation(null)} onDone={refresh}/>}
    <Modal open={Boolean(disburse)} onClose={()=>!busy&&setDisburse(null)} title="Disburse approved loan" description={disburse?.memberName}><form onSubmit={saveDisbursement} className="space-y-4">{error&&<ErrorState message={error}/>}<Field label="Disbursement date" required><input type="date" className={inputClass} required max={disburse?.firstInstallmentDate||undefined} value={disburseForm.disbursementDate} onChange={e=>setDisburseForm({...disburseForm,disbursementDate:e.target.value})}/></Field><Field label="Payment reference" required><input className={inputClass} required value={disburseForm.reference} onChange={e=>setDisburseForm({...disburseForm,reference:e.target.value})}/></Field><Button type="submit" loading={busy}>Confirm disbursement</Button></form></Modal>
  </>;
}
