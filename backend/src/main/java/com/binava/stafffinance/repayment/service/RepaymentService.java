package com.binava.stafffinance.repayment.service;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.approval.service.WorkflowService;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.common.References;
import com.binava.stafffinance.common.dto.BatchView;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanSchedule;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.loan.repository.LoanScheduleRepository;
import com.binava.stafffinance.repayment.dto.RepaymentBatchItemRequest;
import com.binava.stafffinance.repayment.dto.RepaymentBatchRequest;
import com.binava.stafffinance.repayment.dto.RepaymentRequest;
import com.binava.stafffinance.repayment.dto.RepaymentView;
import com.binava.stafffinance.repayment.entity.LoanRepayment;
import com.binava.stafffinance.repayment.entity.PaymentStatus;
import com.binava.stafffinance.repayment.entity.RepaymentBatch;
import com.binava.stafffinance.repayment.mapper.RepaymentMapper;
import com.binava.stafffinance.repayment.repository.RepaymentBatchRepository;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepaymentService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Autowired private com.binava.stafffinance.loan.service.InstallmentScheduleService scheduleService;
    @org.springframework.beans.factory.annotation.Autowired private com.binava.stafffinance.loan.service.LoanSettlementService settlementService;
    @org.springframework.beans.factory.annotation.Autowired private com.binava.stafffinance.repayment.repository.RepaymentAllocationRepository allocations;
    private final LoanRepository loans;
    private final LoanScheduleRepository schedules;
    private final RepaymentRepository repayments;
    private final RepaymentBatchRepository batches;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;

    public RepaymentService(LoanRepository loans, LoanScheduleRepository schedules, RepaymentRepository repayments,
                     RepaymentBatchRepository batches, AuthService auth, WorkflowService workflow, AuditService audit) {
        this.loans = loans;
        this.schedules = schedules;
        this.repayments = repayments;
        this.batches = batches;
        this.auth = auth;
        this.workflow = workflow;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public RepaymentView getRepayment(Long id) {
        LoanRepayment payment=requireRepayment(id);
        AppUser user=auth.currentUser();
        if(user.role==Role.MEMBER && (user.member==null || !user.member.id.equals(payment.loan.member.id)))
            throw new org.springframework.security.access.AccessDeniedException("Members may only view their own repayments");
        return RepaymentMapper.repayment(payment);
    }

    @Transactional(readOnly = true)
    public List<RepaymentView> listRepayments(Long loanId, WorkflowStatus status) {
        List<LoanRepayment> source = loanId == null
                ? repayments.findAllByOrderByPaymentDateDescCreatedAtDesc()
                : repayments.findByLoanIdOrderByPaymentDateDesc(loanId);
        AppUser current = auth.currentUser();
        return source.stream()
                .filter(item -> current.role != Role.MEMBER ||
                        (current.member != null && item.loan.member.id.equals(current.member.id)))
                .filter(item -> status == null || item.workflowStatus == status)
                .map(RepaymentMapper::repayment).toList();
    }

    @Transactional
    public RepaymentView createRepayment(RepaymentRequest request) {
        Loan loan = requireLoan(request.loanId());
        lock(loan);
        if (!"FULL_SETTLEMENT".equals(request.paymentType())) validateRepayment(loan, request.amount());
        AppUser user = auth.currentUser();
        LoanRepayment payment = new LoanRepayment();
        payment.loan = loan;
        payment.amount = Money.amount(request.amount());
        payment.paymentDate = request.paymentDate();
        payment.reference = request.reference() == null || request.reference().isBlank()
                ? References.next("PAY") : request.reference().trim();
        payment.remarks = request.remarks();
        payment.createdBy = user;
        payment.scheduleId=request.scheduleId(); payment.paymentType=request.paymentType()==null?"GENERAL":request.paymentType();
        if(!List.of("GENERAL","INSTALLMENT","FULL_SETTLEMENT").contains(payment.paymentType)) throw new BusinessException("Invalid payment type");
        if("INSTALLMENT".equals(payment.paymentType) && payment.scheduleId==null) throw new BusinessException("Select an installment");
        preparePayment(payment);
        repayments.save(payment);
        audit.log(user, "CREATE", "REPAYMENT", payment.id, payment.reference,
                null, payment.workflowStatus, "Loan repayment prepared");
        return RepaymentMapper.repayment(payment);
    }

    @Transactional
    public RepaymentView submitRepayment(Long id) {
        LoanRepayment payment = requireRepayment(id);
        assertStandaloneRepayment(payment);
        lock(payment.loan); preparePayment(payment);
        workflow.submit(payment, "FULL_SETTLEMENT".equals(payment.paymentType)?"FULL_SETTLEMENT":"REPAYMENT", payment.reference);
        return RepaymentMapper.repayment(payment);
    }

    @Transactional
    public RepaymentView decideRepayment(Long id, DecisionRequest decision) {
        LoanRepayment payment = requireRepayment(id);
        assertStandaloneRepayment(payment);
        lock(payment.loan);
        if (decision.approve()) validatePrepared(payment);
        workflow.decide(payment, decision.approve(), decision.remarks(),
                "REPAYMENT", payment.reference);
        if (decision.approve()) applyPayment(payment);
        return RepaymentMapper.repayment(payment);
    }

    @Transactional(readOnly = true)
    public List<BatchView> listRepaymentBatches() {
        return batches.findAllByOrderByCreatedAtDesc().stream().map(this::batchView).toList();
    }

    @Transactional(readOnly = true)
    public List<RepaymentView> repaymentBatchItems(Long batchId) {
        requireBatch(batchId);
        return repayments.findByBatchIdOrderByLoanMemberFullNameAsc(batchId)
                .stream().map(RepaymentMapper::repayment).toList();
    }

    @Transactional
    public BatchView createRepaymentBatch(RepaymentBatchRequest request) {
        if(request.remarks()==null || request.remarks().isBlank()) throw new BusinessException("Batch description is required");
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
            lock(loan);
            validateRepayment(loan, item.amount());
            LoanRepayment payment = new LoanRepayment();
            payment.loan = loan;
            payment.batch = batch;
            payment.amount = Money.amount(item.amount());
            payment.paymentDate = period.atEndOfMonth();
            payment.reference = "RB-" + batch.id + "-" + loan.applicationNumber;
            payment.remarks = request.remarks();
            payment.createdBy = user; payment.paymentType="INSTALLMENT";
            var due=scheduleService.active(loan).stream().filter(r -> YearMonth.from(r.dueDate).equals(period) && scheduleService.remaining(r).signum()>0 && (item.scheduleId()==null || r.id.equals(item.scheduleId()))).findFirst().orElseThrow(()->new BusinessException("No unpaid installment for this staff member in the selected period"));
            payment.scheduleId=due.id; preparePayment(payment);
            repayments.save(payment);
            total = total.add(payment.amount);
        }
        batch.totalAmount = Money.amount(total);
        audit.log(user, "CREATE", "REPAYMENT_BATCH", batch.id, "RB-" + batch.id,
                null, batch.workflowStatus, request.items().size() + " repayments prepared");
        return batchView(batch);
    }

    @Transactional
    public BatchView submitRepaymentBatch(Long id) {
        RepaymentBatch batch = requireBatch(id);
        workflow.submit(batch, "REPAYMENT_BATCH", "RB-" + batch.id);
        for (LoanRepayment payment : repayments.findByBatchIdOrderByLoanMemberFullNameAsc(id)) {
            lock(payment.loan); preparePayment(payment);
            payment.workflowStatus = WorkflowStatus.PENDING_APPROVAL;
            payment.submittedBy = batch.submittedBy;
            payment.submittedAt = batch.submittedAt;
        }
        return batchView(batch);
    }

    @Transactional
    public BatchView decideRepaymentBatch(Long id, DecisionRequest decision) {
        RepaymentBatch batch = requireBatch(id);
        List<LoanRepayment> items = repayments.findByBatchIdOrderByLoanMemberFullNameAsc(id);
        if (decision.approve()) {
            for (LoanRepayment item : items.stream().sorted(java.util.Comparator.comparing(p->p.loan.id)).toList()) { lock(item.loan); validatePrepared(item); }
        }
        workflow.decide(batch, decision.approve(), decision.remarks(),
                "REPAYMENT_BATCH", "RB-" + batch.id);
        for (LoanRepayment item : items) {
            item.workflowStatus = batch.workflowStatus;
            item.actionedBy = batch.actionedBy;
            item.actionedAt = batch.actionedAt;
            item.decisionRemarks = batch.decisionRemarks;
            if (decision.approve()) applyPayment(item);
        }
        return batchView(batch);
    }

    private Loan requireLoan(Long id) {
        return loans.findById(id).orElseThrow(() -> new NotFoundException("Loan not found"));
    }

    private LoanRepayment requireRepayment(Long id) {
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

    private void validateRepayment(Loan loan, BigDecimal amount) {
        if (loan.adjustmentType != null) throw new BusinessException("Resolve the pending loan change before recording repayments");
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

    private void lock(Loan loan) {
        if(entityManager!=null) entityManager.lock(loan,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Transactional(readOnly=true)
    public Object settlementQuote(Long loanId) { return settlementService.quote(requireLoan(loanId)); }

    @Transactional(readOnly=true)
    public List<com.binava.stafffinance.repayment.dto.DueInstallmentView> dueInstallments(String periodText) {
        YearMonth period;
        try { period=YearMonth.parse(periodText); } catch(Exception e) { throw new BusinessException("Period must use YYYY-MM"); }
        var result=new java.util.ArrayList<com.binava.stafffinance.repayment.dto.DueInstallmentView>();
        for(var loan:loans.findAllByOrderByApplicationDateDescCreatedAtDesc()) {
            if(loan.loanStatus!=LoanStatus.ACTIVE || loan.adjustmentType!=null) continue;
            for(var row:scheduleService.active(loan)) if(YearMonth.from(row.dueDate).equals(period) && scheduleService.remaining(row).signum()>0) {
                BigDecimal interest=Money.amount(row.interestAmount).subtract(row.amountPaid.min(Money.amount(row.interestAmount))).max(BigDecimal.ZERO);
                result.add(new com.binava.stafffinance.repayment.dto.DueInstallmentView(row.id,loan.id,loan.applicationNumber,loan.member.memberCode,loan.member.fullName,
                    row.sequenceNumber==null?row.installmentNumber:row.sequenceNumber,row.dueDate,scheduleService.remaining(row).subtract(interest),interest,scheduleService.remaining(row),loan.outstandingBalance));
            }
        }
        return result;
    }

    private List<LoanSchedule> paymentRows(LoanRepayment payment) {
        return scheduleService.active(payment.loan).stream().filter(r -> payment.scheduleId==null || r.id.equals(payment.scheduleId)).toList();
    }

    private void preparePayment(LoanRepayment payment) {
        if(payment.paymentDate.isBefore(payment.loan.disbursementDate)) throw new BusinessException("Payment date cannot precede disbursement");
        payment.scheduleVersionId=payment.loan.currentScheduleVersionId; payment.outstandingBefore=payment.loan.outstandingBalance;
        if("FULL_SETTLEMENT".equals(payment.paymentType)) {
            var quote=settlementService.quote(payment.loan);
            if(payment.amount.compareTo(quote.settlementAmount())!=0) throw new BusinessException("Settlement quote changed. Review the new amount: "+quote.settlementAmount());
            if(repayments.findByLoanIdOrderByPaymentDateDesc(payment.loan.id).stream().anyMatch(p->!java.util.Objects.equals(p.id,payment.id) && (p.workflowStatus==WorkflowStatus.PENDING_APPROVAL || p.workflowStatus==WorkflowStatus.DRAFT))) throw new BusinessException("Resolve other draft/pending repayments before full settlement");
            payment.principalPaid=quote.remainingPrincipal(); payment.interestPaid=quote.applicableInterest(); payment.chargesPaid=quote.charges(); payment.interestWaived=quote.interestWaived();
            return;
        }
        validateRepayment(payment.loan,payment.amount);
        if(repayments.findByLoanIdOrderByPaymentDateDesc(payment.loan.id).stream().anyMatch(p->!java.util.Objects.equals(p.id,payment.id) && "FULL_SETTLEMENT".equals(p.paymentType) && (p.workflowStatus==WorkflowStatus.PENDING_APPROVAL || p.workflowStatus==WorkflowStatus.DRAFT))) throw new BusinessException("Resolve the pending settlement first");
        var rows=paymentRows(payment);
        if(payment.scheduleId!=null && rows.isEmpty()) throw new BusinessException("Installment is no longer in the active schedule");
        BigDecimal remaining=payment.amount,principal=BigDecimal.ZERO,interest=BigDecimal.ZERO;
        for(var row:rows) {
            BigDecimal allocated=remaining.min(scheduleService.remaining(row));
            if(allocated.signum()<=0) continue;
            if(payment.installmentNumber==null) { payment.installmentNumber=row.sequenceNumber==null?row.installmentNumber:row.sequenceNumber; payment.expectedInstallment=row.expectedAmount; }
            BigDecimal interestPart=Money.amount(row.interestAmount).subtract(row.amountPaid.min(Money.amount(row.interestAmount))).max(BigDecimal.ZERO).min(allocated);
            interest=interest.add(interestPart); principal=principal.add(allocated.subtract(interestPart)); remaining=remaining.subtract(allocated);
        }
        if(remaining.signum()>0) throw new BusinessException("Payment exceeds the unpaid installment amount; this installment may already be paid");
        payment.principalPaid=principal; payment.interestPaid=interest; payment.chargesPaid=Money.amount(BigDecimal.ZERO);
    }

    private void validatePrepared(LoanRepayment payment) {
        BigDecimal principal=payment.principalPaid, interest=payment.interestPaid;
        Long version=payment.scheduleVersionId;
        if(!java.util.Objects.equals(version,payment.loan.currentScheduleVersionId)) throw new BusinessException("Schedule has changed. Reject and prepare a new repayment");
        preparePayment(payment);
        if(principal!=null && (principal.compareTo(payment.principalPaid)!=0 || interest.compareTo(payment.interestPaid)!=0)) throw new BusinessException("Payment allocation changed. Reject and prepare a new repayment");
    }

    private void applyPayment(LoanRepayment payment) {
        Loan loan=payment.loan;
        boolean settlement="FULL_SETTLEMENT".equals(payment.paymentType);
        BigDecimal remaining=payment.amount.subtract(Money.amount(payment.chargesPaid));
        BigDecimal settlementInterest=Money.amount(payment.interestPaid);
        for(var row:paymentRows(payment)) {
            if(!settlement && remaining.signum()<=0) break;
            BigDecimal due=scheduleService.remaining(row);
            if(due.signum()<=0) continue;
            BigDecimal unpaidInterest=Money.amount(row.interestAmount).subtract(row.amountPaid.min(Money.amount(row.interestAmount))).max(BigDecimal.ZERO);
            BigDecimal interest,principal,waived=BigDecimal.ZERO;
            if(settlement) {
                interest=settlementInterest.min(unpaidInterest); settlementInterest=settlementInterest.subtract(interest);
                principal=Money.amount(row.principalAmount).subtract(scheduleService.paidPrincipal(row)); waived=unpaidInterest.subtract(interest);
            } else {
                BigDecimal amount=remaining.min(due); interest=amount.min(unpaidInterest); principal=amount.subtract(interest);
            }
            row.amountPaid=Money.amount(row.amountPaid.add(principal).add(interest)); row.interestWaived=waived;
            remaining=remaining.subtract(principal).subtract(interest);
            row.paymentStatus=settlement?PaymentStatus.SETTLED:scheduleService.remaining(row).signum()==0?PaymentStatus.PAID:PaymentStatus.PARTIAL;
            var allocation=new com.binava.stafffinance.repayment.entity.RepaymentAllocation(); allocation.repaymentId=payment.id; allocation.scheduleId=row.id; allocation.scheduleVersionId=row.scheduleVersionId;
            allocation.installmentNumber=row.sequenceNumber==null?row.installmentNumber:row.sequenceNumber; allocation.principal=principal; allocation.interest=interest; allocation.interestWaived=waived; allocations.save(allocation);
        }
        if(remaining.signum()!=0) throw new BusinessException("Payment could not be allocated completely");
        loan.outstandingBalance=settlement?Money.amount(BigDecimal.ZERO):Money.amount(loan.outstandingBalance.subtract(payment.amount));
        if(settlement) {
            loan.totalInterest=loan.totalInterest.subtract(Money.amount(payment.interestWaived));
            loan.totalPayable=loan.totalPayable.subtract(Money.amount(payment.interestWaived)).add(Money.amount(payment.chargesPaid));
        }
        if(loan.outstandingBalance.signum()==0) { loan.loanStatus=settlement?LoanStatus.CLOSED:LoanStatus.COMPLETED; loan.committedMemberId=null; }
    }

    private BatchView batchView(RepaymentBatch batch) {
        return new BatchView(batch.id, batch.period, batch.totalAmount, batch.workflowStatus,
                batch.createdBy.fullName, batch.actionedBy == null ? null : batch.actionedBy.fullName,
                batch.createdAt, batch.remarks);
    }
}
