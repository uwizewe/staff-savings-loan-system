package com.binava.stafffinance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
class LoanService {
    private final LoanRepository loans;
    private final LoanScheduleRepository schedules;
    private final RepaymentRepository repayments;
    private final RepaymentBatchRepository batches;
    private final MemberService members;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;
    private final SettingService settings;

    LoanService(LoanRepository loans, LoanScheduleRepository schedules,
                RepaymentRepository repayments, RepaymentBatchRepository batches,
                MemberService members, AuthService auth, WorkflowService workflow,
                AuditService audit, SettingService settings) {
        this.loans = loans;
        this.schedules = schedules;
        this.repayments = repayments;
        this.batches = batches;
        this.members = members;
        this.auth = auth;
        this.workflow = workflow;
        this.audit = audit;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    List<LoanView> list(Long memberId, LoanStatus status) {
        AppUser current = auth.currentUser();
        Long effectiveMemberId = memberId;
        if (current.role == Role.MEMBER) {
            if (current.member == null) throw new BusinessException("This account is not linked to a member");
            effectiveMemberId = current.member.id;
        }
        List<Loan> source = effectiveMemberId == null
                ? loans.findAllByOrderByApplicationDateDescCreatedAtDesc()
                : loans.findByMemberIdOrderByApplicationDateDesc(effectiveMemberId);
        return source.stream()
                .filter(loan -> status == null || loan.loanStatus == status)
                .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    LoanView get(Long id) {
        Loan loan = requireLoan(id);
        assertMemberAccess(loan.member.id);
        return view(loan);
    }

    @Transactional(readOnly = true)
    List<ScheduleView> schedule(Long loanId) {
        Loan loan = requireLoan(loanId);
        assertMemberAccess(loan.member.id);
        return schedules.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream().map(Views::schedule).toList();
    }

    @Transactional
    LoanView create(LoanRequest request) {
        Member member = members.require(request.memberId());
        if (member.membershipStatus != MembershipStatus.ACTIVE &&
                !settings.bool("allowLoansForLeftMembers", false)) {
            throw new BusinessException("Only active members may receive a new loan");
        }
        boolean hasActiveLoan = loans.findByMemberIdOrderByApplicationDateDesc(member.id).stream()
                .anyMatch(loan -> loan.loanStatus == LoanStatus.ACTIVE ||
                        loan.loanStatus == LoanStatus.PENDING_APPROVAL || loan.loanStatus == LoanStatus.APPROVED);
        if (hasActiveLoan) throw new BusinessException("This member already has an active or pending loan");

        AppUser user = auth.currentUser();
        Loan loan = new Loan();
        loan.applicationNumber = References.next("LOAN");
        loan.member = member;
        loan.applicationDate = request.applicationDate();
        loan.requestedAmount = Money.amount(request.requestedAmount());
        loan.annualInterestRate = request.annualInterestRate();
        loan.repaymentMonths = request.repaymentMonths();
        loan.purpose = request.purpose().trim();
        loan.remarks = request.remarks();
        loan.createdBy = user;
        loans.save(loan);
        audit.log(user, "CREATE", "LOAN", loan.id, loan.applicationNumber,
                null, loan.loanStatus, "Loan application created for " + member.fullName);
        return view(loan);
    }

    @Transactional
    LoanView submit(Long id) {
        Loan loan = requireLoan(id);
        boolean hasOtherLoan = loans.findByMemberIdOrderByApplicationDateDesc(loan.member.id).stream()
                .anyMatch(other -> !other.id.equals(loan.id) &&
                        (other.loanStatus == LoanStatus.ACTIVE ||
                         other.loanStatus == LoanStatus.PENDING_APPROVAL ||
                         other.loanStatus == LoanStatus.APPROVED));
        if (hasOtherLoan) throw new BusinessException("This member already has an active or pending loan");
        workflow.submit(loan, "LOAN", loan.applicationNumber);
        loan.loanStatus = LoanStatus.PENDING_APPROVAL;
        return view(loan);
    }

    @Transactional
    LoanView approve(Long id, LoanDecisionRequest request) {
        Loan loan = requireLoan(id);
        workflow.decide(loan, true, request.remarks(), "LOAN", loan.applicationNumber);
        BigDecimal principal = Money.amount(request.approvedAmount() == null
                ? loan.requestedAmount : request.approvedAmount());
        BigDecimal rate = request.annualInterestRate() == null
                ? loan.annualInterestRate : request.annualInterestRate();
        int months = request.repaymentMonths() == null ? loan.repaymentMonths : request.repaymentMonths();
        if (principal.compareTo(loan.requestedAmount) > 0) {
            throw new BusinessException("Approved amount cannot exceed the requested amount");
        }
        if (principal.compareTo(BigDecimal.ONE) < 0 || rate.signum() < 0 || months < 1 || months > 120) {
            throw new BusinessException("Loan terms are invalid");
        }

        LoanCalculator.LoanTerms terms = LoanCalculator.flatRate(principal, rate, months);
        loan.approvedAmount = principal;
        loan.annualInterestRate = rate;
        loan.repaymentMonths = months;
        loan.totalInterest = terms.interest();
        loan.totalPayable = terms.totalPayable();
        loan.monthlyInstallment = terms.monthlyInstallment();
        loan.outstandingBalance = terms.totalPayable();
        loan.loanStatus = LoanStatus.APPROVED;
        return view(loan);
    }

    @Transactional
    LoanView reject(Long id, DecisionRequest request) {
        Loan loan = requireLoan(id);
        workflow.decide(loan, false, request.remarks(), "LOAN", loan.applicationNumber);
        loan.loanStatus = LoanStatus.REJECTED;
        return view(loan);
    }

    @Transactional
    LoanView disburse(Long id, DisbursementRequest request) {
        Loan loan = requireLoan(id);
        if (loan.workflowStatus != WorkflowStatus.APPROVED || loan.loanStatus != LoanStatus.APPROVED) {
            throw new BusinessException("Only an approved loan can be disbursed");
        }
        if (!schedules.findByLoanIdOrderByInstallmentNumberAsc(id).isEmpty()) {
            throw new BusinessException("This loan has already been disbursed");
        }
        loan.disbursementDate = request.disbursementDate();
        loan.disbursementReference = request.reference().trim();
        loan.loanStatus = LoanStatus.ACTIVE;
        createSchedule(loan);
        AppUser user = auth.currentUser();
        audit.log(user, "DISBURSE", "LOAN", loan.id, loan.applicationNumber,
                LoanStatus.APPROVED, LoanStatus.ACTIVE,
                "Loan disbursed using reference " + loan.disbursementReference);
        return view(loan);
    }

    @Transactional(readOnly = true)
    List<RepaymentView> listRepayments(Long loanId, WorkflowStatus status) {
        List<LoanRepayment> source = loanId == null
                ? repayments.findAllByOrderByPaymentDateDescCreatedAtDesc()
                : repayments.findByLoanIdOrderByPaymentDateDesc(loanId);
        AppUser current = auth.currentUser();
        return source.stream()
                .filter(item -> current.role != Role.MEMBER ||
                        (current.member != null && item.loan.member.id.equals(current.member.id)))
                .filter(item -> status == null || item.workflowStatus == status)
                .map(Views::repayment).toList();
    }

    @Transactional
    RepaymentView createRepayment(RepaymentRequest request) {
        Loan loan = requireLoan(request.loanId());
        validateRepayment(loan, request.amount());
        AppUser user = auth.currentUser();
        LoanRepayment payment = new LoanRepayment();
        payment.loan = loan;
        payment.amount = Money.amount(request.amount());
        payment.paymentDate = request.paymentDate();
        payment.reference = request.reference() == null || request.reference().isBlank()
                ? References.next("PAY") : request.reference().trim();
        payment.remarks = request.remarks();
        payment.createdBy = user;
        repayments.save(payment);
        audit.log(user, "CREATE", "REPAYMENT", payment.id, payment.reference,
                null, payment.workflowStatus, "Loan repayment prepared");
        return Views.repayment(payment);
    }

    @Transactional
    RepaymentView submitRepayment(Long id) {
        LoanRepayment payment = requireRepayment(id);
        assertStandaloneRepayment(payment);
        workflow.submit(payment, "REPAYMENT", payment.reference);
        return Views.repayment(payment);
    }

    @Transactional
    RepaymentView decideRepayment(Long id, DecisionRequest decision) {
        LoanRepayment payment = requireRepayment(id);
        assertStandaloneRepayment(payment);
        if (decision.approve()) validateRepayment(payment.loan, payment.amount);
        workflow.decide(payment, decision.approve(), decision.remarks(),
                "REPAYMENT", payment.reference);
        if (decision.approve()) applyPayment(payment.loan, payment.amount);
        return Views.repayment(payment);
    }

    @Transactional(readOnly = true)
    List<BatchView> listRepaymentBatches() {
        return batches.findAllByOrderByCreatedAtDesc().stream().map(this::batchView).toList();
    }

    @Transactional(readOnly = true)
    List<RepaymentView> repaymentBatchItems(Long batchId) {
        requireBatch(batchId);
        return repayments.findByBatchIdOrderByLoanMemberFullNameAsc(batchId)
                .stream().map(Views::repayment).toList();
    }

    @Transactional
    BatchView createRepaymentBatch(RepaymentBatchRequest request) {
        YearMonth period;
        try {
            period = YearMonth.parse(request.period());
        } catch (Exception exception) {
            throw new BusinessException("Period must use YYYY-MM format");
        }
        AppUser user = auth.currentUser();
        RepaymentBatch batch = new RepaymentBatch();
        batch.period = period.toString();
        batch.remarks = request.remarks();
        batch.createdBy = user;
        batches.save(batch);

        Set<Long> loanIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RepaymentBatchItemRequest item : request.items()) {
            if (!loanIds.add(item.loanId())) {
                throw new BusinessException("A loan may appear only once in a monthly batch");
            }
            Loan loan = requireLoan(item.loanId());
            validateRepayment(loan, item.amount());
            LoanRepayment payment = new LoanRepayment();
            payment.loan = loan;
            payment.batch = batch;
            payment.amount = Money.amount(item.amount());
            payment.paymentDate = period.atEndOfMonth();
            payment.reference = "RB-" + batch.id + "-" + loan.applicationNumber;
            payment.remarks = "Monthly repayment for " + period;
            payment.createdBy = user;
            repayments.save(payment);
            total = total.add(payment.amount);
        }
        batch.totalAmount = Money.amount(total);
        audit.log(user, "CREATE", "REPAYMENT_BATCH", batch.id, "RB-" + batch.id,
                null, batch.workflowStatus, request.items().size() + " repayments prepared");
        return batchView(batch);
    }

    @Transactional
    BatchView submitRepaymentBatch(Long id) {
        RepaymentBatch batch = requireBatch(id);
        workflow.submit(batch, "REPAYMENT_BATCH", "RB-" + batch.id);
        for (LoanRepayment payment : repayments.findByBatchIdOrderByLoanMemberFullNameAsc(id)) {
            payment.workflowStatus = WorkflowStatus.PENDING_APPROVAL;
            payment.submittedBy = batch.submittedBy;
            payment.submittedAt = batch.submittedAt;
        }
        return batchView(batch);
    }

    @Transactional
    BatchView decideRepaymentBatch(Long id, DecisionRequest decision) {
        RepaymentBatch batch = requireBatch(id);
        List<LoanRepayment> items = repayments.findByBatchIdOrderByLoanMemberFullNameAsc(id);
        if (decision.approve()) {
            for (LoanRepayment item : items) validateRepayment(item.loan, item.amount);
        }
        workflow.decide(batch, decision.approve(), decision.remarks(),
                "REPAYMENT_BATCH", "RB-" + batch.id);
        for (LoanRepayment item : items) {
            item.workflowStatus = batch.workflowStatus;
            item.actionedBy = batch.actionedBy;
            item.actionedAt = batch.actionedAt;
            item.decisionRemarks = batch.decisionRemarks;
            if (decision.approve()) applyPayment(item.loan, item.amount);
        }
        return batchView(batch);
    }

    Loan requireLoan(Long id) {
        return loans.findById(id).orElseThrow(() -> new NotFoundException("Loan not found"));
    }

    LoanRepayment requireRepayment(Long id) {
        return repayments.findById(id).orElseThrow(() -> new NotFoundException("Repayment not found"));
    }

    private RepaymentBatch requireBatch(Long id) {
        return batches.findById(id).orElseThrow(() -> new NotFoundException("Repayment batch not found"));
    }

    private void assertStandaloneRepayment(LoanRepayment payment) {
        if (payment.batch != null) {
            throw new BusinessException("Submit or decide this repayment through its monthly batch");
        }
    }

    private void createSchedule(Loan loan) {
        List<BigDecimal> amounts = LoanCalculator.schedule(loan.totalPayable, loan.repaymentMonths);
        for (int number = 1; number <= loan.repaymentMonths; number++) {
            LoanSchedule row = new LoanSchedule();
            row.loan = loan;
            row.installmentNumber = number;
            row.dueDate = loan.disbursementDate.plusMonths(number);
            row.expectedAmount = amounts.get(number - 1);
            row.amountPaid = BigDecimal.ZERO.setScale(2);
            row.paymentStatus = PaymentStatus.PENDING;
            schedules.save(row);
        }
    }

    private void validateRepayment(Loan loan, BigDecimal amount) {
        if (loan.loanStatus != LoanStatus.ACTIVE) {
            throw new BusinessException("Repayments can only be recorded for active loans");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Repayment amount must be greater than zero");
        }
        if (Money.amount(amount).compareTo(Money.amount(loan.outstandingBalance)) > 0) {
            throw new BusinessException("Repayment cannot exceed the outstanding balance of " + loan.outstandingBalance);
        }
    }

    private void applyPayment(Loan loan, BigDecimal paymentAmount) {
        BigDecimal remaining = Money.amount(paymentAmount);
        for (LoanSchedule row : schedules.findByLoanIdOrderByInstallmentNumberAsc(loan.id)) {
            if (remaining.signum() <= 0) break;
            BigDecimal due = row.expectedAmount.subtract(row.amountPaid).max(BigDecimal.ZERO);
            BigDecimal allocation = remaining.min(due);
            row.amountPaid = Money.amount(row.amountPaid.add(allocation));
            remaining = remaining.subtract(allocation);
            row.paymentStatus = row.amountPaid.compareTo(row.expectedAmount) >= 0
                    ? PaymentStatus.PAID : PaymentStatus.PARTIAL;
        }
        loan.outstandingBalance = Money.amount(loan.outstandingBalance.subtract(paymentAmount)
                .max(BigDecimal.ZERO));
        if (loan.outstandingBalance.signum() == 0) loan.loanStatus = LoanStatus.COMPLETED;
    }

    private LoanView view(Loan loan) {
        BigDecimal repaid = repayments.findByLoanIdOrderByPaymentDateDesc(loan.id).stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Views.loan(loan, repaid);
    }

    private BatchView batchView(RepaymentBatch batch) {
        return new BatchView(batch.id, batch.period, batch.totalAmount, batch.workflowStatus,
                batch.createdBy.fullName, batch.actionedBy == null ? null : batch.actionedBy.fullName,
                batch.createdAt, batch.remarks);
    }

    private void assertMemberAccess(Long memberId) {
        AppUser user = auth.currentUser();
        if (user.role == Role.MEMBER && (user.member == null || !user.member.id.equals(memberId))) {
            throw new org.springframework.security.access.AccessDeniedException("Members can only view their own loans");
        }
    }
}
