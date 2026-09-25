package com.binava.stafffinance.loan.service;
import com.binava.stafffinance.loan.repository.LoanRepository;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds version metadata and the known equal-principal split of legacy flat-rate schedules. */
@Service
public class LoanScheduleUpgradeService {
    private final LoanRepository loans;
    private final InstallmentScheduleService schedules;
    private final com.binava.stafffinance.audit.service.AuditService audit;
    public LoanScheduleUpgradeService(LoanRepository loans,InstallmentScheduleService schedules,com.binava.stafffinance.audit.service.AuditService audit) { this.loans=loans; this.schedules=schedules; this.audit=audit; }
    @Transactional
    public void upgrade() {
        for(var loan:loans.findAllByOrderByApplicationDateDescCreatedAtDesc()) {
            // Existing independently approved loans follow the same activation flow as new approvals.
            if(loan.loanStatus==com.binava.stafffinance.loan.entity.LoanStatus.APPROVED
                    && loan.workflowStatus==com.binava.stafffinance.approval.entity.WorkflowStatus.APPROVED) {
                loan.disbursementDate=loan.actionedAt==null?java.time.LocalDate.now():loan.actionedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                loan.disbursementReference="APPROVAL-"+loan.applicationNumber;
                if(schedules.active(loan).isEmpty()) {
                    var version=schedules.propose(loan,loan.approvedAmount,loan.repaymentMonths,
                        loan.firstInstallmentDate==null?loan.disbursementDate.plusMonths(1):loan.firstInstallmentDate,
                        "APPROVED_TERMS",loan.disbursementDate,BigDecimal.ZERO,"Activation of previously approved loan",loan.createdBy);
                    schedules.activate(loan,version,loan.actionedBy,loan.decisionRemarks);
                }
                loan.loanStatus=com.binava.stafffinance.loan.entity.LoanStatus.ACTIVE;
                loan.committedMemberId=loan.member.id;
                audit.log(loan.actionedBy,"ACTIVATE","LOAN",loan.id,loan.applicationNumber,"APPROVED","ACTIVE","Automatic activation under approval-based loan workflow");
            }
            var rows=schedules.active(loan);
            if(rows.isEmpty()) continue;
            if(loan.originalPrincipal==null) loan.originalPrincipal=loan.requestedAmount;
            if(loan.originalTerm==null) loan.originalTerm=loan.repaymentMonths;
            if(loan.firstInstallmentDate==null) loan.firstInstallmentDate=rows.get(0).dueDate;
            if(rows.stream().allMatch(r->r.principalAmount==null) && loan.approvedAmount!=null
                    && rows.size()==loan.repaymentMonths && !"REDUCING_BALANCE".equals(loan.interestMethod)
                    && rows.stream().map(r->r.expectedAmount).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(loan.totalPayable)==0) {
                BigDecimal regular=loan.approvedAmount.divide(BigDecimal.valueOf(rows.size()),2,RoundingMode.HALF_UP);
                BigDecimal balance=loan.approvedAmount;
                boolean valid=true;
                for(int i=0;i<rows.size();i++) {
                    BigDecimal principal=i==rows.size()-1?balance:regular;
                    if(principal.signum()<0 || rows.get(i).expectedAmount.compareTo(principal)<0) { valid=false; break; }
                    balance=balance.subtract(principal);
                }
                if(valid) {
                    balance=loan.approvedAmount;
                    for(int i=0;i<rows.size();i++) {
                        var row=rows.get(i); row.openingBalance=balance;
                        row.principalAmount=i==rows.size()-1?balance:regular;
                        row.interestAmount=row.expectedAmount.subtract(row.principalAmount);
                        balance=balance.subtract(row.principalAmount); row.closingBalance=balance;
                    }
                    loan.interestMethod="FLAT_LEGACY";
                }
            }
            for(var row:rows) if(row.openingBalance==null && row.principalAmount!=null && row.closingBalance!=null) row.openingBalance=row.principalAmount.add(row.closingBalance);
            schedules.ensureBaseline(loan);
            if(loan.activePrincipal==null) loan.activePrincipal=rows.stream().map(r->r.principalAmount==null?BigDecimal.ZERO:r.principalAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        }
    }
}
