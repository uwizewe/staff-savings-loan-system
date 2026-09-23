package com.binava.stafffinance.loan.mapper;

import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.loan.dto.LoanView;
import com.binava.stafffinance.loan.dto.ScheduleView;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanSchedule;
import java.math.BigDecimal;

public final class LoanMapper {
    private LoanMapper() {}

    public static LoanView loan(Loan loan, BigDecimal amountRepaid) {
        return loan(loan,amountRepaid,null);
    }
    public static LoanView loan(Loan loan, BigDecimal amountRepaid, BigDecimal paidInterest) {
        return loan(loan, amountRepaid, paidInterest, 0, 0);
    }
    public static LoanView loan(Loan loan, BigDecimal amountRepaid, BigDecimal paidInterest, int paid, int remaining) {
        return loan(loan,amountRepaid,paidInterest,paid,remaining,BigDecimal.ZERO);
    }
    public static LoanView loan(Loan loan, BigDecimal amountRepaid, BigDecimal paidInterest, int paid, int remaining, BigDecimal charges) {
        BigDecimal principalPaid=paidInterest==null?null:Money.amount(amountRepaid.subtract(paidInterest).subtract(charges));
        return new LoanView(loan.id, loan.applicationNumber, loan.member.id, loan.member.fullName,
                loan.applicationDate, loan.requestedAmount, loan.approvedAmount,
                loan.annualInterestRate, loan.repaymentMonths, loan.purpose,
                loan.monthlyInstallment, loan.totalInterest, loan.totalPayable,
                Money.amount(amountRepaid), Money.amount(loan.outstandingBalance), loan.workflowStatus,
                loan.loanStatus, loan.disbursementDate, loan.disbursementReference,
                loan.createdBy.fullName, loan.actionedBy == null ? null : loan.actionedBy.fullName,
                loan.remarks, loan.createdAt, loan.categoryName, loan.interestMethod, loan.adjustmentType, loan.adjustmentAmount, loan.adjustmentMonths, loan.adjustmentDate, loan.adjustmentRequestedName, principalPaid, paidInterest, loan.adjustmentRemarks, loan.member.memberCode, loan.originalPrincipal==null?loan.requestedAmount:loan.originalPrincipal,
                loan.activePrincipal==null?loan.approvedAmount:loan.activePrincipal,
                principalPaid==null?null:Money.amount(loan.approvedAmount==null?loan.requestedAmount:loan.approvedAmount).subtract(principalPaid).max(BigDecimal.ZERO),
                loan.originalTerm==null?loan.repaymentMonths:loan.originalTerm,loan.firstInstallmentDate,loan.currentScheduleVersionId,paid,remaining);
    }

    public static ScheduleView schedule(LoanSchedule schedule) {
        return new ScheduleView(schedule.id, schedule.sequenceNumber==null?schedule.installmentNumber:schedule.sequenceNumber, schedule.dueDate,
                schedule.expectedAmount, schedule.amountPaid,
                Money.amount(schedule.expectedAmount.subtract(schedule.amountPaid).subtract(Money.amount(schedule.interestWaived)).max(BigDecimal.ZERO)),
                schedule.paymentStatus == com.binava.stafffinance.repayment.entity.PaymentStatus.SETTLED ? schedule.paymentStatus :
                    schedule.expectedAmount.compareTo(schedule.amountPaid)>0 && schedule.dueDate.isBefore(java.time.LocalDate.now()) ? com.binava.stafffinance.repayment.entity.PaymentStatus.OVERDUE : schedule.paymentStatus,
                schedule.principalAmount, schedule.interestAmount, schedule.closingBalance, schedule.openingBalance, schedule.scheduleVersionId);
    }
}
