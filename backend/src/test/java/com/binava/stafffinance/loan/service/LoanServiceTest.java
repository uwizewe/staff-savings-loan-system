package com.binava.stafffinance.loan.service;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.service.WorkflowService;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.config.service.SettingService;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.loan.dto.LoanDecisionRequest;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.loan.repository.LoanScheduleRepository;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.service.MemberService;
import com.binava.stafffinance.repayment.entity.LoanRepayment;
import com.binava.stafffinance.repayment.entity.RepaymentBatch;
import com.binava.stafffinance.repayment.repository.RepaymentBatchRepository;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.repayment.service.RepaymentService;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanServiceTest {
    private final LoanRepository loans = mock(LoanRepository.class);
    private final RepaymentRepository repayments = mock(RepaymentRepository.class);
    private final LoanScheduleRepository schedules = mock(LoanScheduleRepository.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private final InstallmentScheduleService scheduleService = mock(InstallmentScheduleService.class);
    private LoanService service;
    private RepaymentService repaymentService;

    @BeforeEach
    void setUp() {
        service = new LoanService(loans, schedules, repayments,
                mock(MemberService.class), mock(AuthService.class), workflow,
                mock(AuditService.class), mock(SettingService.class));
        repaymentService = new RepaymentService(loans, schedules, repayments, mock(RepaymentBatchRepository.class), mock(AuthService.class), workflow, mock(AuditService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"scheduleService",scheduleService);
        org.springframework.test.util.ReflectionTestUtils.setField(repaymentService,"scheduleService",scheduleService);
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
        assertThrows(BusinessException.class, () -> repaymentService.submitRepayment(1L));
        assertThrows(BusinessException.class,
                () -> repaymentService.decideRepayment(1L, new DecisionRequest(true, "Approve")));
        assertThrows(BusinessException.class,
                () -> repaymentService.decideRepayment(1L, new DecisionRequest(false, "Reject")));
        verifyNoInteractions(workflow, schedules);
    }

    @Test
    void standaloneRepaymentCanStillBeSubmitted() {
        LoanRepayment payment = new LoanRepayment();
        payment.loan = loan(1L);
        payment.createdBy = payment.loan.createdBy;
        payment.reference = "PAY-1";
        payment.loan.loanStatus=LoanStatus.ACTIVE;
        payment.loan.disbursementDate=java.time.LocalDate.of(2026,1,1);
        payment.loan.outstandingBalance=new BigDecimal("100");
        payment.paymentDate=java.time.LocalDate.of(2026,2,1);
        payment.amount=new BigDecimal("10");
        var row=new com.binava.stafffinance.loan.entity.LoanSchedule();
        row.id=1L; row.loan=payment.loan; row.expectedAmount=new BigDecimal("100"); row.amountPaid=BigDecimal.ZERO; row.interestAmount=BigDecimal.ZERO;
        when(scheduleService.active(payment.loan)).thenReturn(List.of(row));
        when(scheduleService.remaining(row)).thenReturn(new BigDecimal("100"));
        when(repayments.findById(1L)).thenReturn(Optional.of(payment));
        assertNull(repaymentService.submitRepayment(1L).batchId());
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
