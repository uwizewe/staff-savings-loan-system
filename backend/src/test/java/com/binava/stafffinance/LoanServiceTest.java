package com.binava.stafffinance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanServiceTest {
    private final LoanRepository loans = mock(LoanRepository.class);
    private final RepaymentRepository repayments = mock(RepaymentRepository.class);
    private final LoanScheduleRepository schedules = mock(LoanScheduleRepository.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(loans, schedules, repayments, mock(RepaymentBatchRepository.class),
                mock(MemberService.class), mock(AuthService.class), workflow,
                mock(AuditService.class), mock(SettingService.class));
    }

    @Test
    void blocksSubmittingDraftWhenAnotherLoanIsPendingApprovedOrActive() {
        Loan draft = loan(1L);
        Loan other = loan(2L);
        when(loans.findById(1L)).thenReturn(Optional.of(draft));
        when(loans.findByMemberIdOrderByApplicationDateDesc(10L)).thenReturn(List.of(draft, other));
        for (LoanStatus status : List.of(LoanStatus.PENDING_APPROVAL, LoanStatus.APPROVED, LoanStatus.ACTIVE)) {
            other.loanStatus = status;
            assertThrows(BusinessException.class, () -> service.submit(1L));
            assertEquals(LoanStatus.DRAFT, draft.loanStatus);
        }
        verifyNoInteractions(workflow);
    }

    @Test
    void allowsSubmissionWithOnlyCompletedOrRejectedOtherLoans() {
        Loan draft = loan(1L);
        Loan other = loan(2L);
        when(loans.findById(1L)).thenReturn(Optional.of(draft));
        when(loans.findByMemberIdOrderByApplicationDateDesc(10L)).thenReturn(List.of(draft, other));
        for (LoanStatus status : List.of(LoanStatus.COMPLETED, LoanStatus.REJECTED, LoanStatus.DRAFT)) {
            other.loanStatus = status;
            assertDoesNotThrow(() -> service.submit(1L));
        }
        verify(workflow, times(3)).submit(draft, "LOAN", draft.applicationNumber);
    }

    @Test
    void batchItemsCannotBeSubmittedOrDecidedIndividually() {
        LoanRepayment payment = new LoanRepayment();
        payment.batch = new RepaymentBatch();
        when(repayments.findById(1L)).thenReturn(Optional.of(payment));
        assertThrows(BusinessException.class, () -> service.submitRepayment(1L));
        assertThrows(BusinessException.class,
                () -> service.decideRepayment(1L, new DecisionRequest(true, "Approve")));
        assertThrows(BusinessException.class,
                () -> service.decideRepayment(1L, new DecisionRequest(false, "Reject")));
        verifyNoInteractions(workflow, schedules);
    }

    @Test
    void standaloneRepaymentCanStillBeSubmitted() {
        LoanRepayment payment = new LoanRepayment();
        payment.loan = loan(1L);
        payment.createdBy = payment.loan.createdBy;
        payment.reference = "PAY-1";
        when(repayments.findById(1L)).thenReturn(Optional.of(payment));
        assertNull(service.submitRepayment(1L).batchId());
        verify(workflow).submit(payment, "REPAYMENT", "PAY-1");
    }

    @Test
    void approvalRejectsTermsOutsideSupportedLimits() {
        Loan loan = loan(1L);
        when(loans.findById(1L)).thenReturn(Optional.of(loan));
        assertThrows(BusinessException.class, () -> service.approve(1L,
                new LoanDecisionRequest(null, null, null, 121)));
        assertThrows(BusinessException.class, () -> service.approve(1L,
                new LoanDecisionRequest(null, new BigDecimal("0.99"), null, 12)));
    }

    private Loan loan(long id) {
        Loan loan = new Loan();
        loan.id = id;
        loan.member = new Member();
        loan.member.id = 10L;
        loan.member.fullName = "Test Member";
        loan.createdBy = new AppUser();
        loan.createdBy.fullName = "Test Initiator";
        loan.applicationNumber = "LOAN-" + id;
        loan.requestedAmount = new BigDecimal("1000.00");
        loan.annualInterestRate = new BigDecimal("12.00");
        loan.repaymentMonths = 12;
        return loan;
    }
}
