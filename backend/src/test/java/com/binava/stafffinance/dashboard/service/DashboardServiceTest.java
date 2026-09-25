package com.binava.stafffinance.dashboard.service;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.approval.service.ApprovalService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.finance.entity.FinanceTransaction;
import com.binava.stafffinance.finance.entity.FinanceType;
import com.binava.stafffinance.finance.repository.FinanceRepository;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.member.repository.MemberRepository;
import com.binava.stafffinance.repayment.entity.LoanRepayment;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.savings.repository.SavingsRepository;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    final MemberRepository members=mock(MemberRepository.class);
    final SavingsRepository savings=mock(SavingsRepository.class);
    final LoanRepository loans=mock(LoanRepository.class);
    final RepaymentRepository repayments=mock(RepaymentRepository.class);
    final FinanceRepository finance=mock(FinanceRepository.class);
    final ApprovalService approvals=mock(ApprovalService.class);
    final AuthService auth=mock(AuthService.class);
    final DashboardService service=new DashboardService(members,savings,loans,repayments,finance,approvals,auth);

    @Test void incomeIncludesApprovedInterestAndChargesButNotPrincipalOrPendingPayments() {
        var user=new AppUser(); user.role=Role.ADMIN; when(auth.currentUser()).thenReturn(user);
        var receipt=new FinanceTransaction();receipt.financeType=FinanceType.INCOME;receipt.workflowStatus=WorkflowStatus.APPROVED;receipt.amount=new BigDecimal("200");receipt.transactionDate=LocalDate.now();
        when(finance.findAll()).thenReturn(List.of(receipt));
        var paid=new LoanRepayment();paid.workflowStatus=WorkflowStatus.APPROVED;paid.amount=new BigDecimal("115");paid.principalPaid=new BigDecimal("100");paid.interestPaid=new BigDecimal("10");paid.chargesPaid=new BigDecimal("5");paid.paymentDate=LocalDate.now();
        var pending=new LoanRepayment();pending.workflowStatus=WorkflowStatus.PENDING_APPROVAL;pending.amount=new BigDecimal("999");pending.interestPaid=new BigDecimal("999");pending.paymentDate=LocalDate.now();
        when(repayments.findAll()).thenReturn(List.of(paid,pending));
        var result=service.dashboard();assertEquals(0,result.totalIncome().compareTo(new BigDecimal("215")));
        assertEquals(0,((BigDecimal)result.monthlyTrend().get(5).get("income")).compareTo(new BigDecimal("215")));
        assertEquals(0,result.totalLoanRepayments().compareTo(new BigDecimal("115")));
    }
    @Test void memberWithoutLinkedProfileDoesNotReceiveAssociationBalances() {
        var user=new AppUser();user.role=Role.MEMBER;when(auth.currentUser()).thenReturn(user);
        var result=service.dashboard();assertEquals(0,result.totalMembers());assertEquals(0,result.activeLoans());assertEquals(0,result.totalSavings().signum());
        verify(savings,never()).findAll();verify(loans,never()).findAll();verify(finance,never()).findAll();
    }
}
