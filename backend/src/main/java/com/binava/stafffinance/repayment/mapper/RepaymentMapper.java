package com.binava.stafffinance.repayment.mapper;

import com.binava.stafffinance.repayment.dto.RepaymentView;
import com.binava.stafffinance.repayment.entity.LoanRepayment;

public final class RepaymentMapper {
    private RepaymentMapper() {}

    public static RepaymentView repayment(LoanRepayment repayment) {
        return new RepaymentView(repayment.id, repayment.loan.id, repayment.loan.applicationNumber,
                repayment.loan.member.id, repayment.loan.member.fullName, repayment.amount,
                repayment.paymentDate, repayment.reference, repayment.workflowStatus,
                repayment.createdBy.fullName,
                repayment.actionedBy == null ? null : repayment.actionedBy.fullName,
                repayment.remarks, repayment.createdAt,
                repayment.batch == null ? null : repayment.batch.id, repayment.loan.member.memberCode,repayment.installmentNumber,repayment.expectedInstallment,repayment.principalPaid,repayment.interestPaid,repayment.outstandingBefore,repayment.paymentType,repayment.chargesPaid);
    }
}
