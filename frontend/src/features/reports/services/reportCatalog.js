export const reportGroups = [
  { label: 'Reports', items: [['vsa-account','VSA Account Report'],['all-transactions','All Transactions Report'],['member-statement','Member Statement']] },
  { label: 'Savings Reports', items: [['savings','Savings Report'],['savings-transactions','Savings Transactions'],['monthly-savings','Monthly Savings']] },
  { label: 'Loan Reports', items: [['loans','Loan Report'],['active-loans','Active Loans'],['closed-loans','Closed Loans'],['repayments','Loan Repayments'],['outstanding','Outstanding Loans'],['overdue','Overdue / Arrears']] },
  { label: 'Finance Reports', items: [['income-expense','Income & Expense Report'],['daily','Daily Transaction Report'],['monthly-summary','Monthly Financial Summary']] },
  { label: 'Administration', items: [['audit','Audit Log Report']] },
];
export const reportTitles = Object.fromEntries(reportGroups.flatMap(group => group.items));
export const loanReports = ['loans','active-loans','closed-loans','outstanding'];
export const transactionTypes = ['Monthly Saving','Savings Deposit','Savings Withdrawal','Loan Repayment','Loan Principal','Loan Interest','Income','Expense','Penalty','Adjustment'];
export const moneyColumn = (key, label) => ({ key, label, type: 'money' });
export const column = (key, label, type = 'text') => ({ key, label, type });
export const cashColumns = [column('date','Date','date'),column('reference','Reference'),column('type','Transaction'),column('description','Description'),moneyColumn('credit','Money In'),moneyColumn('debit','Money Out'),moneyColumn('balance','Balance')];
export const savingsColumns = [column('date','Date','date'),column('reference','Reference'),column('memberName','Member'),column('type','Transaction'),column('description','Comment'),moneyColumn('amount','Amount'),moneyColumn('balance','Balance'),column('status','Status','status')];
export const loanColumns = [column('loanNumber','Loan No.'),column('memberName','Member'),moneyColumn('amount','Loan Amount'),moneyColumn('principalPaid','Principal Paid'),moneyColumn('interestPaid','Interest Paid'),moneyColumn('outstanding','Outstanding'),column('status','Status','status')];
