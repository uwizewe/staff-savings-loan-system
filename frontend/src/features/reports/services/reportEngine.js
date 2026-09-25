import { cashColumns, column as col, loanColumns, moneyColumn as money, reportTitles, savingsColumns } from './reportCatalog.js';

const cents = value => Math.round(Number(value || 0) * 100);
const sum = (rows, key) => rows.reduce((n, row) => n + (row[key] || 0), 0);
const approved = row => row.status === 'APPROVED';
const byDate = (a, b) => String(a.date).localeCompare(String(b.date)) || String(a.id).localeCompare(String(b.id), undefined, { numeric: true });
const text = value => String(value ?? '').toLocaleLowerCase();
const same = (a, b) => String(a) === String(b);
export const localDate = value => {
  if (!value) return '';
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
};
export const monthRange = month => {
  const [year, number] = month.split('-').map(Number);
  return { from: `${month}-01`, to: `${month}-${new Date(year,number,0).getDate()}` };
};
const inRange = (date, filters) => Boolean(date && (!filters.from || date >= filters.from) && (!filters.to || date <= filters.to));
const memberMatch = (row, filters) => !filters.memberId || same(row.memberId,filters.memberId);
const searchMatch = (row, filters) => !filters.search?.trim() || Object.values(row).some(value => typeof value !== 'object' && text(value).includes(text(filters.search.trim())));
const matching = (row, filters) => memberMatch(row,filters) && searchMatch(row,filters) && (!filters.status || row.status === filters.status) && (!filters.type || row.type === filters.type || filters.type === 'Loan Repayment' && row.source === 'repayment');
const monthlyComment = date => `${new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric' }).format(new Date(`${date}T12:00:00`))} Monthly Saving`;

/** All financial aggregation uses integer cents. Source records are never modified. */
export function normalizeReportData(data) {
  const members = data.members || [], loans = data.loans || [], versions = data.scheduleVersions || {};
  const warnings = new Set();
  const savings = (data.savings || []).map(row => {
    const withdrawal = row.savingType === 'WITHDRAWAL';
    const type = withdrawal ? 'Savings Withdrawal' : row.savingType === 'MONTHLY' ? 'Monthly Saving' : row.savingType === 'ADJUSTMENT' ? 'Adjustment' : 'Savings Deposit';
    return { ...row, id:`saving-${row.id}`, memberCode:members.find(m=>same(m.id,row.memberId))?.memberCode, date:row.transactionDate, type, source:'savings', amount:cents(row.amount)*(withdrawal?-1:1), credit:withdrawal?0:cents(row.amount), debit:withdrawal?cents(row.amount):0,
      description:row.savingType==='MONTHLY'&&(!row.description||row.description.startsWith('Monthly savings for'))?monthlyComment(row.transactionDate):row.description||'' };
  }).sort(byDate);
  const payments = [], rowPayments = [], used = new Map();
  const rawPayments = [...(data.repayments || [])].sort((a,b)=>a.paymentDate.localeCompare(b.paymentDate)||a.id-b.id);
  for (const row of rawPayments) {
    const details = data.repaymentDetails?.[row.id] || {};
    const paid = approved(row), fees = cents(row.chargesPaid), penalty = cents(row.penaltyPaid);
    let principal = row.principalPaid == null ? null : cents(row.principalPaid);
    let interest = row.interestPaid == null ? null : cents(row.interestPaid);
    const recorded = (data.allocations || []).filter(a=>same(a.repaymentId,row.id));
    const history = [...(versions[row.loanId] || [])].filter(v=>v.status==='APPROVED').sort((a,b)=>a.versionNumber-b.versionNumber);
    const version = history.find(v=>same(v.id,details.scheduleVersionId)) || history[0];
    let allocations = recorded.map(a=>({scheduleId:a.scheduleId,principal:cents(a.principal),interest:cents(a.interest),waived:cents(a.interestWaived)}));
    if (paid && !recorded.length && version?.rows?.length && version.rows.every(r=>r.principalAmount!=null&&r.interestAmount!=null)) {
      let remaining = cents(row.amount)-fees-penalty;
      let settlementInterest = interest || 0;
      for (const installment of version.rows) {
        const previous = used.get(installment.id) || { principal:0,interest:0,waived:0 };
        const interestDue = Math.max(0,cents(installment.interestAmount)-previous.interest-previous.waived);
        const principalDue = Math.max(0,cents(installment.principalAmount)-previous.principal);
        const settling = row.paymentType==='FULL_SETTLEMENT';
        const allocationInterest = Math.min(settling?settlementInterest:remaining,interestDue);
        const allocationPrincipal = settling?principalDue:Math.min(Math.max(0,remaining-allocationInterest),principalDue);
        if(allocationInterest||allocationPrincipal||settling) allocations.push({scheduleId:installment.id,principal:allocationPrincipal,interest:allocationInterest,waived:settling?interestDue-allocationInterest:0});
        remaining-=allocationInterest+allocationPrincipal; settlementInterest-=allocationInterest;
        if(!settling&&remaining<=0)break;
      }
      if(remaining!==0) allocations=[];
      if(principal==null&&allocations.length) { principal=sum(allocations,'principal'); interest=sum(allocations,'interest'); }
    }
    if(paid) for(const allocation of allocations) {
      const previous=used.get(allocation.scheduleId)||{principal:0,interest:0,waived:0};
      used.set(allocation.scheduleId,{principal:previous.principal+allocation.principal,interest:previous.interest+allocation.interest,waived:previous.waived+allocation.waived});
      rowPayments.push({...allocation,date:row.paymentDate,repaymentId:row.id});
    }
    const unallocated = principal==null||interest==null ? cents(row.amount)-fees-penalty : cents(row.amount)-principal-interest-fees-penalty;
    if(paid&&unallocated) warnings.add('Some legacy repayments lack a complete principal/interest breakdown. Their cash is included; unknown income allocations are shown separately, not guessed.');
    payments.push({...row,id:`repayment-${row.id}`,sourceId:row.id,date:row.paymentDate,source:'repayment',type:'Loan Repayment',description:row.remarks||'',principal,interest,penalty,fees,unallocated,
      amount:cents(row.amount),credit:cents(row.amount),debit:0,waived:cents(details.interestWaived),actionedAt:details.actionedAt,scheduleVersionId:details.scheduleVersionId??version?.id});
  }
  const funding=[];
  for(const loan of loans) {
    if(!loan.disbursementDate)continue;
    funding.push({id:`funding-${loan.id}`,date:loan.disbursementDate,reference:loan.disbursementReference||loan.applicationNumber,loanId:loan.id,memberId:loan.memberId,memberName:loan.memberName,type:'Loan Principal',source:'funding',description:`Loan funding · ${loan.applicationNumber}`,credit:0,debit:cents(loan.originalPrincipal??loan.approvedAmount),status:'APPROVED'});
    for(const version of versions[loan.id]||[]) if(version.status==='APPROVED'&&Number(version.additionalAmount)>0) funding.push({id:`funding-version-${version.id}`,date:version.effectiveDate,reference:`${loan.applicationNumber}/V${version.versionNumber}`,loanId:loan.id,memberId:loan.memberId,memberName:loan.memberName,type:'Loan Principal',source:'funding',description:`${version.scheduleType.replaceAll('_',' ')} · ${version.remarks||loan.applicationNumber}`,credit:0,debit:cents(version.additionalAmount),status:'APPROVED'});
  }
  const finance=(data.finance||[]).map(row=>({...row,id:`finance-${row.id}`,date:row.transactionDate,type:row.financeType==='INCOME'?'Income':'Expense',source:'finance',credit:row.financeType==='INCOME'?cents(row.amount):0,debit:row.financeType==='EXPENSE'?cents(row.amount):0,amount:cents(row.amount)}));
  const ledger=[...savings,...payments,...finance,...funding].map(row=>({...row})).sort(byDate);
  let accountBalance=0;
  for(const row of ledger){if(approved(row))accountBalance+=row.credit-row.debit;row.balance=accountBalance;}
  const savingBalance=new Map();
  for(const row of savings){const balance=(savingBalance.get(row.memberId)||0)+(approved(row)?row.amount:0);savingBalance.set(row.memberId,balance);row.balance=balance;}
  return {members,loans,versions,savings,payments,rowPayments,funding,finance,ledger,warnings};
}

function positions(model,asOf) {
  return model.loans.map(loan=>{
    const history=[...(model.versions[loan.id]||[])].filter(v=>v.status==='APPROVED'&&v.effectiveDate<=asOf).sort((a,b)=>a.effectiveDate.localeCompare(b.effectiveDate)||a.versionNumber-b.versionNumber);
    const version=history.at(-1);
    const repayments=model.payments.filter(p=>same(p.loanId,loan.id)&&approved(p)&&p.date<=asOf);
    const funded=model.funding.filter(f=>same(f.loanId,loan.id)&&f.date<=asOf);
    const allocated=model.rowPayments.filter(p=>p.date<=asOf);
    const rows=(version?.rows||[]).map(row=>{
      const paid=allocated.filter(p=>same(p.scheduleId,row.id));
      const amountPaid=sum(paid,'principal')+sum(paid,'interest');
      return {...row,expected:cents(row.expectedAmount),paid:amountPaid,remaining:Math.max(0,cents(row.expectedAmount)-amountPaid-sum(paid,'waived')),
        remainingPrincipal:row.principalAmount==null?null:Math.max(0,cents(row.principalAmount)-sum(paid,'principal')),
        remainingInterest:row.interestAmount==null?null:Math.max(0,cents(row.interestAmount)-sum(paid,'interest')-sum(paid,'waived'))};
    });
    const unknown=repayments.some(p=>p.unallocated);
    const outstanding=rows.length?sum(rows,'remaining'):cents(loan.outstandingBalance);
    const status=funded.length?(outstanding===0?'CLOSED':'ACTIVE'):loan.loanStatus==='ACTIVE'||['CLOSED','COMPLETED'].includes(loan.loanStatus)?'PENDING_APPROVAL':loan.loanStatus;
    return {...loan,id:loan.id,memberId:loan.memberId,loanNumber:loan.applicationNumber,date:loan.applicationDate,
      amount:funded.length?sum(funded,'debit'):cents(loan.requestedAmount),original:cents(loan.originalPrincipal??loan.requestedAmount),
      principalPaid:unknown?null:sum(repayments,'principal'),interestPaid:unknown?null:sum(repayments,'interest'),totalPaid:sum(repayments,'amount'),
      remainingPrincipal:rows.some(r=>r.remainingPrincipal==null)?null:rows.length?sum(rows,'remainingPrincipal'):funded.length?Math.max(0,sum(funded,'debit')-sum(repayments,'principal')):0,
      remainingInterest:rows.some(r=>r.remainingInterest==null)?null:rows.length?sum(rows,'remainingInterest'):Math.max(0,outstanding-cents(loan.remainingPrincipal)),
      outstanding:funded.length?outstanding:0,installment:cents(version?.rows?.[0]?.expectedAmount??loan.monthlyInstallment),status,rows,
      totalInterest:unknown?null:sum(repayments,'interest')+sum(rows,'remainingInterest'),startDate:loan.disbursementDate,
      closedDate:status==='CLOSED'?repayments.map(p=>p.date).sort().at(-1)||null:null,nextPayment:rows.filter(r=>r.remaining>0).map(r=>r.dueDate).sort()[0]||null};
  });
}

function incomeRows(model) {
  const result=model.finance.filter(approved).map(row=>({...row,category:row.category||'Other Income',recordedBy:row.createdBy}));
  for(const payment of model.payments.filter(approved)) for(const [key,category] of [['interest','Loan Interest'],['penalty','Penalties'],['fees','Other Income']]) {
    if(!payment[key])continue;
    result.push({...payment,id:`${payment.id}-${key}`,type:'Income',category,amount:payment[key],recordedBy:payment.createdBy,description:`${category} · ${payment.loanNumber} · ${payment.description}`});
  }
  return result.sort(byDate);
}

/** Returns export-ready values; sections and summaries are identical for screen, print, PDF and Excel. */
export function buildReport(type, data, filters = {}) {
  const model=normalizeReportData(data);
  const asOf=filters.to||localDate(data.generatedAt||new Date().toISOString());
  const loanPositions=positions(model,asOf);
  const report={type,title:reportTitles[type]||'Report',organization:data.organization||'VFR Association',currency:data.currency||'RWF',generatedAt:data.generatedAt||new Date().toISOString(),filters:{...filters},summary:[],sections:[],notes:[]};
  const total=(label,value,isMoney=true)=>({label,value,money:isMoney});
  const section=(title,columns,rows,totals=[])=>({title,columns,rows,totals});
  const filter=rows=>rows.filter(r=>inRange(r.date,filters)&&matching(r,filters));
  const cash=model.ledger.filter(approved);
  const cashOpening=sum(cash.filter(r=>filters.from&&r.date<filters.from),'credit')-sum(cash.filter(r=>filters.from&&r.date<filters.from),'debit');
  const cashRows=filter(cash);
  const cashSummary=[total('Opening Balance',cashOpening),total('Total Money In',sum(cashRows,'credit')),total('Total Money Out',sum(cashRows,'debit')),total('Closing Balance',cashOpening+sum(cashRows,'credit')-sum(cashRows,'debit'))];

  if(type==='vsa-account'||type==='daily') {
    // Recalculate the selected ledger balance from all prior recorded transactions.
    let balance=cashOpening;
    const period=cash.filter(r=>inRange(r.date,filters)).map(r=>({...r,balance:balance+=r.credit-r.debit}));
    const rows=period.filter(r=>matching(r,filters));
    report.summary=type==='daily'?[total('Total Money In',sum(rows,'credit')),total('Total Money Out',sum(rows,'debit')),total('Net Movement',sum(rows,'credit')-sum(rows,'debit'))]:cashSummary;
    // Search changes displayed movement totals; account balances stay tied to the full period.
    if(type==='vsa-account'&&filters.search){report.summary[3]=total('Closing Balance',balance);report.notes.push('Search filters rows and movement totals; opening/closing balances represent the full account period.');}
    report.sections=[section(report.title,cashColumns,rows,[total('Money In',sum(rows,'credit')),total('Money Out',sum(rows,'debit'))])];
    report.notes.push('Approved cash movements only. Loan funding and approved top-ups are money out. Repayment totals enter cash once; principal is not income. Opening balance is calculated from recorded history, starting at zero.');
  }

  if(type==='all-transactions') {
    const rows=[];let balance=0;
    for(const item of model.ledger) {
      const parts=item.source==='repayment'?[['Loan Principal',item.principal],['Loan Interest',item.interest],['Penalty',item.penalty],['Income',item.fees],['Loan Repayment',item.unallocated]].filter(([,value])=>value):[[item.type,item.credit||item.debit]];
      for(const [part,value] of parts) {
        const row={...item,id:`${item.id}-${part}`,type:part,credit:item.source==='repayment'?value:item.credit,debit:item.source==='repayment'?0:item.debit};
        if(approved(item))balance+=row.credit-row.debit;
        row.balance=balance;if(inRange(row.date,filters)&&matching(row,filters))rows.push(row);
      }
    }
    report.summary=[total('Approved Debits',sum(rows.filter(approved),'debit')),total('Approved Credits',sum(rows.filter(approved),'credit')),total('Transactions',new Set(rows.map(r=>r.reference)).size,false)];
    report.sections=[section(report.title,[col('date','Date','date'),col('reference','Reference'),col('memberName','Member'),col('type','Transaction Type'),col('description','Description'),money('debit','Debit'),money('credit','Credit'),money('balance','Balance'),col('status','Status','status')],rows)];
    report.notes.push('Debit means money out; credit means money in. Repayments are split into their components without adding a second total row. Only approved records affect the account balance; running balances include earlier and filtered-out transactions.');
  }

  if(type==='savings'||type==='monthly-savings') {
    const through=model.savings.filter(r=>approved(r)&&r.date<=asOf);
    let rows=model.members.filter(m=>(!m.joiningDate||m.joiningDate<=asOf)&&memberMatch({memberId:m.id},filters)).map(member=>{
      const all=through.filter(r=>same(r.memberId,member.id));
      const period=all.filter(r=>inRange(r.date,filters));
      const expected=cents(member.monthlySavingAmount);
      const paid=sum(period.filter(r=>r.savingType==='MONTHLY'),'amount');
      return {id:member.id,memberId:member.id,memberCode:member.memberCode,memberName:member.fullName,monthly:expected,
        deposits:sum(period,'credit'),withdrawals:sum(period,'debit'),balance:sum(all,'amount'),status:member.membershipStatus,
        expected,paid,difference:expected-paid,pending:Math.max(0,expected-paid),paymentStatus:paid>=expected?'PAID':paid>0?'PARTIAL':'PENDING',exitDate:member.exitDate};
    });
    if(type==='monthly-savings')rows=rows.filter(r=>r.status==='ACTIVE'||r.paid>0||r.exitDate&&r.exitDate>=filters.from).map(r=>({...r,status:r.paymentStatus}));
    rows=rows.filter(r=>searchMatch(r,filters)&&(!filters.status||r.status===filters.status));
    if(type==='savings') {
      report.summary=[total('Total Members Saving',rows.filter(r=>r.balance>0).length,false),total('Total Savings',sum(rows,'balance')),total('Total Deposits',sum(rows,'deposits')),total('Total Withdrawals',sum(rows,'withdrawals'))];
      report.sections=[section(report.title,[col('memberCode','Member Code'),col('memberName','Member'),money('monthly','Monthly Saving'),money('deposits','Total Deposits'),money('withdrawals','Total Withdrawals'),money('balance','Current Balance'),col('status','Status','status'),col('actions','Statement','savings-action')],rows)];
      report.notes.push('Deposits and withdrawals use the selected range; balances include all approved savings through the To Date. Monthly saving is the member’s current configured amount.');
    } else {
      report.summary=[total('Expected Monthly Savings',sum(rows,'expected')),total('Amount Collected',sum(rows,'paid')),total('Amount Pending',sum(rows,'pending')),total('Members Paid',rows.filter(r=>r.paid>=r.expected).length,false),total('Members Pending',rows.filter(r=>r.paid<r.expected).length,false)];
      report.sections=[section(report.title,[col('memberName','Member'),money('expected','Expected Saving'),money('paid','Amount Paid'),money('difference','Difference'),col('status','Status','status')],rows)];
      report.notes.push('Only approved MONTHLY savings count as collected. Expected amounts use current member contribution settings; historical rate changes are not stored.');
    }
  }

  if(type==='savings-transactions') {
    const rows=filter(model.savings);
    report.summary=[total('Deposits',sum(rows.filter(approved),'credit')),total('Withdrawals',sum(rows.filter(approved),'debit')),total('Net Savings Movement',sum(rows.filter(approved),'amount'))];
    report.sections=[section(report.title,savingsColumns,rows)];
    report.notes.push('Balance is each member’s running approved savings balance, including transactions before the reporting period.');
  }

  if(['loans','active-loans','closed-loans','outstanding'].includes(type)) {
    const rows=loanPositions.filter(row=>inRange(type==='closed-loans'?row.closedDate:row.date,filters)&&memberMatch(row,filters)&&searchMatch(row,filters)&&(!filters.status||row.status===filters.status)&&
      (type==='loans'||type==='closed-loans'&&row.status==='CLOSED'||type==='active-loans'&&row.status==='ACTIVE'||type==='outstanding'&&row.status==='ACTIVE'&&row.outstanding>0));
    let columns=loanColumns;
    report.summary=[total('Total Loans',rows.length,false),total('Total Loan Amount',sum(rows,'amount')),total('Total Principal Paid',sum(rows,'principalPaid')),total('Total Interest Paid',sum(rows,'interestPaid')),total('Outstanding Principal',sum(rows,'remainingPrincipal')),total('Outstanding Balance',sum(rows,'outstanding'))];
    if(type==='active-loans')columns=[col('loanNumber','Loan No.'),col('memberName','Member'),money('amount','Loan Amount'),money('installment','Installment'),money('principalPaid','Principal Paid'),money('interestPaid','Interest Paid'),money('outstanding','Remaining Balance'),col('nextPayment','Next Payment','date')];
    if(type==='closed-loans')columns=[col('loanNumber','Loan No.'),col('memberName','Member'),money('amount','Loan Amount'),money('interestPaid','Total Interest'),money('totalPaid','Total Paid'),col('startDate','Start Date','date'),col('closedDate','Closed Date','date')];
    if(type==='outstanding') {
      columns=[col('memberName','Member'),col('loanNumber','Loan No.'),money('original','Original Loan'),money('principalPaid','Principal Paid'),money('remainingPrincipal','Remaining Principal'),money('remainingInterest','Remaining Interest'),money('outstanding','Total Outstanding')];
      report.summary=[total('Total Outstanding Principal',sum(rows,'remainingPrincipal')),total('Total Outstanding Interest',sum(rows,'remainingInterest')),total('Total Outstanding Balance',sum(rows,'outstanding'))];
    }
    report.sections=[section(report.title,columns,rows)];
    report.notes.push(type==='closed-loans'?'Date range selects final repayment dates. Closed date is the last approved repayment date.':'Date range selects loan application dates. Balances and loan status are calculated through the To Date.');
  }

  if(type==='repayments') {
    const rows=filter(model.payments).map(row=>({...row,balance:row.outstandingBefore==null?null:approved(row)?Math.max(0,cents(row.outstandingBefore)-row.amount+row.fees+row.penalty-row.waived):cents(row.outstandingBefore)}));
    const cols=[col('date','Date','date'),col('reference','Reference'),col('memberName','Member'),col('loanNumber','Loan No.'),money('principal','Principal'),money('interest','Interest'),money('penalty','Penalty'),money('amount','Total Paid'),money('balance','Balance'),col('status','Status','status')];
    if(rows.some(r=>r.fees))cols.splice(7,0,money('fees','Other Charges'));
    if(rows.some(r=>r.unallocated))cols.splice(7,0,money('unallocated','Unallocated'));
    const paid=rows.filter(approved);
    report.summary=[total('Principal',sum(paid,'principal')),total('Interest',sum(paid,'interest')),total('Penalty',sum(paid,'penalty')),total('Total Paid',sum(paid,'amount'))];
    report.sections=[section(report.title,cols,rows)];
    report.notes.push('Repayment = principal + interest + penalty. Any existing settlement charges are shown separately as Other Charges, not relabelled as penalties. Only approved repayments contribute to totals.');
  }

  if(type==='overdue') {
    const rows=[];
    for(const loan of loanPositions.filter(l=>l.status==='ACTIVE'))for(const row of loan.rows)if(row.remaining>0&&row.dueDate<=asOf&&inRange(row.dueDate,filters)) {
      const days=Math.max(0,Math.floor((Date.parse(`${asOf}T00:00:00Z`)-Date.parse(`${row.dueDate}T00:00:00Z`))/86400000));
      const item={id:row.id,loanId:loan.id,memberId:loan.memberId,memberName:loan.memberName,loanNumber:loan.loanNumber,date:row.dueDate,expected:row.expected,paid:row.paid,overdue:row.remaining,days,status:days>30?'SERIOUSLY_OVERDUE':days>0?'OVERDUE':'DUE'};
      if(matching(item,filters))rows.push(item);
    }
    report.summary=[total('Number of Overdue Loans',new Set(rows.filter(r=>r.days>0).map(r=>r.loanId)).size,false),total('Total Overdue Amount',sum(rows.filter(r=>r.days>0),'overdue'))];
    report.sections=[section(report.title,[col('memberName','Member'),col('loanNumber','Loan No.'),col('date','Due Date','date'),money('expected','Expected Installment'),money('paid','Paid'),money('overdue','Overdue Amount'),col('days','Days Overdue','number'),col('status','Status','status')],rows)];
    report.notes.push('Due = due on the To Date; Overdue = 1–30 days late; Seriously Overdue = more than 30 days late. Replaced schedules are excluded. Date range filters installment due dates.');
  }

  if(type==='income-expense') {
    const rows=filter(incomeRows(model));
    const income=sum(rows.filter(r=>r.type==='Income'),'amount'),expenses=sum(rows.filter(r=>r.type==='Expense'),'amount');
    report.summary=[total('Total Income',income),total('Total Expenses',expenses),total('Net Surplus / Deficit',income-expenses)];
    report.sections=[section(report.title,[col('date','Date','date'),col('reference','Reference'),col('category','Category'),col('type','Type'),col('description','Description'),money('amount','Amount'),col('recordedBy','Recorded By')],rows)];
    report.notes.push('Income includes approved loan interest and penalties. Savings and loan principal are excluded from income and expenses. Settlement charges are Other Income. Finance entries are separate posted transactions; a supporting reference alone does not reverse or deduplicate a posting.');
  }

  if(type==='member-statement') {
    const member=model.members.find(m=>same(m.id,filters.memberId));
    if(!member){report.notes.push('Select one member and Generate to prepare the two separate statements.');}
    else {
      report.member={code:member.memberCode,name:member.fullName,department:member.department,status:member.membershipStatus};
      const ownSavings=model.savings.filter(r=>same(r.memberId,member.id)),active=loanPositions.filter(l=>same(l.memberId,member.id)&&l.status==='ACTIVE');
      report.summary=[total('Monthly Saving',cents(member.monthlySavingAmount)),total('Total Savings',sum(ownSavings.filter(r=>approved(r)&&r.date<=asOf),'amount')),total('Active Loan',active.length,false),total('Loan Amount',sum(active,'amount')),total('Principal Paid',sum(active,'principalPaid')),total('Interest Paid',sum(active,'interestPaid')),total('Remaining Balance',sum(active,'outstanding'))];
      const loanRows=[];
      for(const loan of model.loans.filter(l=>same(l.memberId,member.id))) {
        const history=(model.versions[loan.id]||[]).filter(v=>v.status==='APPROVED');
        const scheduleEvents=history.map(v=>({id:`schedule-${v.id}`,date:v.versionNumber===1?loan.disbursementDate:v.effectiveDate,reference:`${loan.applicationNumber}/V${v.versionNumber}`,type:v.versionNumber===1?'Loan funding':v.scheduleType.replaceAll('_',' '),principal:cents(v.principal),interest:sum(v.rows.map(r=>({interest:cents(r.interestAmount)})),'interest'),amount:0,reset:sum(v.rows.map(r=>({expected:cents(r.expectedAmount)})),'expected'),order:v.versionNumber*2})).filter(r=>r.date);
        const events=[...scheduleEvents,...model.payments.filter(r=>same(r.loanId,loan.id)&&approved(r)).map(r=>({...r,change:-(r.amount-r.fees-r.penalty+r.waived),order:(history.find(v=>same(v.id,r.scheduleVersionId))?.versionNumber||1)*2+1}))].sort((a,b)=>a.date.localeCompare(b.date)||a.order-b.order||byDate(a,b));
        let balance=0;
        for(const event of events){balance=event.reset??(balance+event.change);const row={...event,loanNumber:loan.applicationNumber,balance:Math.max(0,balance)};if(inRange(row.date,filters)&&searchMatch(row,filters))loanRows.push(row);}
      }
      report.sections=[section('Savings Statement',savingsColumns.filter(c=>!['reference','memberName'].includes(c.key)),filter(ownSavings)),section('Loan Statement',[col('date','Date','date'),col('loanNumber','Loan No.'),col('type','Transaction'),money('principal','Principal'),money('interest','Interest'),money('amount','Amount Paid'),money('balance','Remaining Balance')],loanRows)];
      report.notes.push('Savings and loans are separate statements. Loan balances include scheduled principal and interest. An approved schedule change replaces the remaining schedule; waived interest reduces the balance without becoming a cash payment.');
    }
  }

  if(type==='monthly-summary') {
    const period=rows=>rows.filter(r=>inRange(r.date,filters));
    const financial=period(incomeRows(model)),periodPayments=period(model.payments).filter(approved),periodSavings=period(model.savings).filter(approved);
    const interest=sum(financial.filter(r=>r.category==='Loan Interest'),'amount'),other=sum(financial.filter(r=>r.type==='Income'&&r.category!=='Loan Interest'),'amount'),expenses=sum(financial.filter(r=>r.type==='Expense'),'amount');
    const figures=[total('Total Savings',sum(model.savings.filter(r=>approved(r)&&r.date<=asOf),'amount')),total('Savings Collected This Month',sum(periodSavings,'credit')),total('Outstanding Loans',sum(loanPositions.filter(l=>l.status==='ACTIVE'),'outstanding')),total('Loan Repayments',sum(periodPayments,'amount')),total('Interest Income',interest),total('Other Income',other),total('Expenses',expenses),total('Available Cash',sum(cash.filter(r=>r.date<=asOf),'credit')-sum(cash.filter(r=>r.date<=asOf),'debit')),total('Net Surplus',interest+other-expenses)];
    report.summary=figures;
    report.sections=[section('Monthly Financial Summary',[col('metric','Metric'),money('amount','Amount')],figures.map((f,i)=>({id:i,metric:f.label,amount:f.value})).filter(r=>searchMatch(r,filters)))];
    report.notes.push('Savings, outstanding loans and cash are balances through the To Date. Collections, repayments, income, expenses and surplus cover the selected month/range. Cash starts from recorded history at zero.');
  }

  if(type==='audit') {
    const rows=(data.audits||[]).map(r=>({...r,date:localDate(r.createdAt),dateTime:r.createdAt,module:r.entityType,description:r.details})).filter(r=>inRange(r.date,filters)&&searchMatch(r,filters)&&(!filters.user||r.user===filters.user)&&(!filters.module||r.module===filters.module)&&(!filters.action||r.action===filters.action));
    report.summary=[total('Actions',rows.length,false),total('Users',new Set(rows.map(r=>r.user)).size,false)];
    report.sections=[section(report.title,[col('dateTime','Date & Time','datetime'),col('user','User'),col('action','Action'),col('module','Module'),col('reference','Reference'),col('description','Description')],rows)];
  }

  if(filters.from&&filters.to&&filters.from>filters.to)throw new Error('From Date must be on or before To Date.');
  if(!['audit','savings','monthly-savings','savings-transactions'].includes(type)) report.notes.push(...model.warnings);
  if(model.payments.some(p=>approved(p)&&p.fees)&&['income-expense','repayments','monthly-summary'].includes(type))report.notes.push('The current repayment model stores settlement charges, not a distinct penalty field. Separately recorded finance penalties remain income; they are not assigned to a loan without a stored allocation.');
  const convertTotal=item=>({...item,value:item.money?item.value/100:item.value});
  report.summary=report.summary.map(convertTotal);
  report.sections=report.sections.map(s=>({...s,totals:s.totals.map(convertTotal),rows:s.rows.map(r=>{
    const converted={...r};for(const c of s.columns)if(c.type==='money')converted[c.key]=r[c.key]==null?null:r[c.key]/100;return converted;
  })}));
  return report;
}
