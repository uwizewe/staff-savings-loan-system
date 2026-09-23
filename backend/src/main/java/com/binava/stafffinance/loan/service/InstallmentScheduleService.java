package com.binava.stafffinance.loan.service;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.loan.dto.*;
import com.binava.stafffinance.loan.entity.*;
import com.binava.stafffinance.loan.mapper.LoanMapper;
import com.binava.stafffinance.loan.repository.*;
import com.binava.stafffinance.repayment.entity.PaymentStatus;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Owns schedule versions. Historical rows are never deleted or reused for payments. */
@Service
public class InstallmentScheduleService {
    private final LoanScheduleRepository rows;
    private final LoanScheduleVersionRepository versions;
    private final LoanCalculationService calculator;

    public InstallmentScheduleService(LoanScheduleRepository rows, LoanScheduleVersionRepository versions, LoanCalculationService calculator) {
        this.rows=rows; this.versions=versions; this.calculator=calculator;
    }

    public List<LoanSchedule> active(Loan loan) {
        return rows.findByLoanIdOrderByInstallmentNumberAsc(loan.id).stream()
                .filter(row -> Objects.equals(row.scheduleVersionId,loan.currentScheduleVersionId)).toList();
    }

    public BigDecimal interestPaid(Loan loan) {
        var active=active(loan);
        if(active.stream().anyMatch(row -> row.interestAmount==null)) return null;
        return Money.amount(loan.interestPaidBeforeSchedule).add(active.stream()
                .map(row -> row.amountPaid.min(row.interestAmount.subtract(Money.amount(row.interestWaived)))).reduce(BigDecimal.ZERO,BigDecimal::add));
    }

    public BigDecimal principalPaid(Loan loan) {
        return Money.amount(loan.principalPaidBeforeSchedule).add(active(loan).stream()
                .map(this::paidPrincipal).reduce(BigDecimal.ZERO,BigDecimal::add));
    }

    public BigDecimal paidPrincipal(LoanSchedule row) {
        return row.principalAmount==null?BigDecimal.ZERO:row.amountPaid.subtract(Money.amount(row.interestAmount).subtract(Money.amount(row.interestWaived))).max(BigDecimal.ZERO).min(row.principalAmount);
    }

    public BigDecimal remainingPrincipal(Loan loan) {
        return active(loan).stream().map(row -> Money.amount(row.principalAmount).subtract(paidPrincipal(row)))
                .reduce(BigDecimal.ZERO,BigDecimal::add);
    }

    public int paidCount(Loan loan) {
        return (loan.installmentsPaidBeforeSchedule==null?0:loan.installmentsPaidBeforeSchedule)+(int)active(loan).stream()
                .filter(row -> row.paymentStatus==PaymentStatus.PAID || row.paymentStatus==PaymentStatus.SETTLED).count();
    }

    public int remainingCount(Loan loan) {
        return (int)active(loan).stream().filter(row -> remaining(row).signum()>0).count();
    }

    public BigDecimal remaining(LoanSchedule row) {
        return row.expectedAmount.subtract(row.amountPaid).subtract(Money.amount(row.interestWaived)).max(BigDecimal.ZERO);
    }

    /** Attach metadata to pre-versioning rows, retaining all original amounts and dates. */
    public void ensureBaseline(Loan loan) {
        if(loan.currentScheduleVersionId!=null) return;
        var existing=active(loan);
        if(existing.isEmpty()) return;
        var version=new LoanScheduleVersion();
        version.loanId=loan.id; version.versionNumber=1; version.scheduleType="LEGACY_BASELINE";
        version.effectiveDate=loan.disbursementDate==null?loan.applicationDate:loan.disbursementDate;
        version.firstInstallmentDate=existing.get(0).dueDate;
        version.principal=loan.approvedAmount==null?loan.requestedAmount:loan.approvedAmount;
        version.previousPrincipal=version.principal; version.additionalAmount=BigDecimal.ZERO;
        version.term=existing.size(); version.annualRate=loan.annualInterestRate;
        version.interest=loan.totalInterest; version.totalPayable=loan.totalPayable; version.installment=loan.monthlyInstallment;
        version.createdBy=loan.createdBy; version.actionedBy=loan.actionedBy; version.actionedAt=loan.actionedAt;
        version.workflowStatus=WorkflowStatus.APPROVED;
        version.remarks="Preserved schedule present at the versioning upgrade; earlier replaced schedules cannot be reconstructed.";
        versions.save(version);
        for(var row:existing) { row.scheduleVersionId=version.id; row.sequenceNumber=row.installmentNumber; }
        loan.currentScheduleVersionId=version.id;
    }

    public LoanScheduleVersion propose(Loan loan, BigDecimal principal, int term, LocalDate firstDate,
            String type, LocalDate effectiveDate, BigDecimal additional, String remarks, AppUser creator) {
        ensureBaseline(loan);
        var calculation=calculator.calculate(principal,loan.annualInterestRate,term,firstDate);
        var version=new LoanScheduleVersion(); version.loanId=loan.id;
        version.versionNumber=versions.findByLoanIdOrderByVersionNumberDesc(loan.id).stream().mapToInt(v->v.versionNumber).max().orElse(0)+1;
        version.scheduleType=type; version.effectiveDate=effectiveDate; version.firstInstallmentDate=firstDate;
        version.previousPrincipal=remainingPrincipal(loan); version.previousTerm=remainingCount(loan);
        version.additionalAmount=additional; version.principal=calculation.principal(); version.annualRate=loan.annualInterestRate;
        version.term=term; version.interest=calculation.interest(); version.installment=calculation.monthlyInstallment(); version.totalPayable=calculation.totalPayable();
        version.createdBy=creator; version.remarks=remarks; versions.save(version);
        int offset=rows.findByLoanIdOrderByInstallmentNumberAsc(loan.id).stream().mapToInt(r->r.installmentNumber).max().orElse(0);
        for(var item:calculation.rows()) {
            var row=new LoanSchedule(); row.loan=loan; row.scheduleVersionId=version.id;
            row.installmentNumber=offset+item.number(); row.sequenceNumber=item.number(); row.dueDate=item.dueDate();
            row.openingBalance=item.openingBalance(); row.principalAmount=item.principal(); row.interestAmount=item.interest();
            row.expectedAmount=item.amount(); row.closingBalance=item.closingBalance(); row.amountPaid=Money.amount(BigDecimal.ZERO);
            rows.save(row);
        }
        return version;
    }

    public void activate(Loan loan, LoanScheduleVersion version, AppUser approver, String remarks) {
        loan.principalPaidBeforeSchedule=principalPaid(loan);
        loan.interestPaidBeforeSchedule=Money.amount(interestPaid(loan));
        loan.installmentsPaidBeforeSchedule=paidCount(loan);
        loan.currentScheduleVersionId=version.id;
        loan.activePrincipal=version.principal;
        loan.interestMethod="REDUCING_BALANCE";
        loan.firstInstallmentDate=version.firstInstallmentDate;
        loan.totalInterest=loan.interestPaidBeforeSchedule.add(version.interest);
        loan.monthlyInstallment=version.installment;
        loan.totalPayable=Money.amount(loan.approvedAmount).add(loan.totalInterest);
        loan.outstandingBalance=version.totalPayable;
        loan.repaymentMonths=version.term;
        version.workflowStatus=WorkflowStatus.APPROVED; version.actionedBy=approver; version.actionedAt=Instant.now(); version.decisionRemarks=remarks;
    }

    public LoanScheduleVersion requireVersion(Long id) {
        return versions.findById(id).orElseThrow(()->new com.binava.stafffinance.exception.NotFoundException("Schedule version not found"));
    }

    public List<ScheduleVersionView> history(Loan loan) {
        return versions.findByLoanIdOrderByVersionNumberDesc(loan.id).stream().map(v->new ScheduleVersionView(v.id,v.versionNumber,v.scheduleType,
            v.workflowStatus.name(),Objects.equals(v.id,loan.currentScheduleVersionId),v.effectiveDate,v.firstInstallmentDate,
            v.previousPrincipal,v.additionalAmount,v.principal,v.previousTerm,v.term,v.annualRate,
            v.createdBy.fullName,v.createdAt,v.submittedBy==null?null:v.submittedBy.fullName,v.submittedAt,
            v.actionedBy==null?null:v.actionedBy.fullName,v.actionedAt,v.remarks,v.decisionRemarks,
            rows.findByLoanIdOrderByInstallmentNumberAsc(loan.id).stream().filter(r->Objects.equals(r.scheduleVersionId,v.id)).map(LoanMapper::schedule).toList())).toList();
    }
}
