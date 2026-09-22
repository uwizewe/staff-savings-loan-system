package com.binava.stafffinance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
class SavingsService {
    private final SavingsRepository savings;
    private final SavingsBatchRepository batches;
    private final MemberService members;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;
    private final SettingService settings;

    SavingsService(SavingsRepository savings, SavingsBatchRepository batches, MemberService members,
                   AuthService auth, WorkflowService workflow, AuditService audit,
                   SettingService settings) {
        this.savings = savings;
        this.batches = batches;
        this.members = members;
        this.auth = auth;
        this.workflow = workflow;
        this.audit = audit;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    List<SavingView> list(Long memberId, SavingType type, WorkflowStatus status) {
        AppUser current = auth.currentUser();
        Long effectiveMemberId = memberId;
        if (current.role == Role.MEMBER) {
            if (current.member == null) throw new BusinessException("This account is not linked to a member");
            effectiveMemberId = current.member.id;
        }
        List<SavingsTransaction> source = effectiveMemberId == null
                ? savings.findAllByOrderByTransactionDateDescCreatedAtDesc()
                : savings.findByMemberIdOrderByTransactionDateDesc(effectiveMemberId);
        return source.stream()
                .filter(item -> type == null || item.savingType == type)
                .filter(item -> status == null || item.workflowStatus == status)
                .map(Views::saving).toList();
    }

    @Transactional(readOnly = true)
    List<BatchView> listBatches() {
        return batches.findAllByOrderByCreatedAtDesc().stream().map(Views::savingsBatch).toList();
    }

    @Transactional(readOnly = true)
    List<SavingView> batchItems(Long batchId) {
        requireBatch(batchId);
        return savings.findByBatchIdOrderByMemberFullNameAsc(batchId).stream().map(Views::saving).toList();
    }

    @Transactional
    SavingView create(SavingRequest request, SavingType type) {
        if (type != SavingType.INDIVIDUAL && type != SavingType.WITHDRAWAL && type != SavingType.ADJUSTMENT) {
            throw new BusinessException("Unsupported individual savings type");
        }
        Member member = members.require(request.memberId());
        if (type != SavingType.WITHDRAWAL && member.membershipStatus != MembershipStatus.ACTIVE) {
            throw new BusinessException("Normal savings can only be recorded for active members");
        }
        if (type == SavingType.WITHDRAWAL) validateWithdrawal(member.id, request.amount());

        AppUser user = auth.currentUser();
        SavingsTransaction transaction = new SavingsTransaction();
        transaction.member = member;
        transaction.savingType = type;
        transaction.amount = Money.amount(request.amount());
        transaction.transactionDate = request.transactionDate();
        transaction.reference = request.reference() == null || request.reference().isBlank()
                ? References.next(type == SavingType.WITHDRAWAL ? "WDL" : "SAV")
                : request.reference().trim();
        transaction.description = request.description();
        transaction.createdBy = user;
        savings.save(transaction);
        audit.log(user, "CREATE", "SAVING", transaction.id, transaction.reference,
                null, transaction.workflowStatus, type + " saving transaction created");
        return Views.saving(transaction);
    }

    @Transactional
    BatchView createBatch(SavingsBatchRequest request) {
        AppUser user = auth.currentUser();
        YearMonth period;
        try {
            period = YearMonth.parse(request.period());
        } catch (Exception exception) {
            throw new BusinessException("Period must use YYYY-MM format");
        }

        SavingsBatch batch = new SavingsBatch();
        batch.period = period.toString();
        batch.remarks = request.remarks();
        batch.createdBy = user;
        batches.save(batch);

        BigDecimal total = BigDecimal.ZERO;
        java.util.Set<Long> memberIds = new java.util.HashSet<>();
        for (SavingsBatchItemRequest item : request.items()) {
            if (!memberIds.add(item.memberId())) {
                throw new BusinessException("A member may appear only once in a monthly batch");
            }
            Member member = members.require(item.memberId());
            if (member.membershipStatus != MembershipStatus.ACTIVE) {
                throw new BusinessException(member.fullName + " is not an active member");
            }
            SavingsTransaction transaction = new SavingsTransaction();
            transaction.member = member;
            transaction.batch = batch;
            transaction.savingType = SavingType.MONTHLY;
            transaction.amount = Money.amount(item.amount());
            transaction.transactionDate = period.atEndOfMonth();
            transaction.reference = "SB-" + batch.id + "-" + member.memberCode;
            transaction.description = "Monthly savings for " + period;
            transaction.createdBy = user;
            savings.save(transaction);
            total = total.add(transaction.amount);
        }
        batch.totalAmount = Money.amount(total);
        audit.log(user, "CREATE", "SAVINGS_BATCH", batch.id, "SB-" + batch.id,
                null, batch.workflowStatus, request.items().size() + " monthly contributions prepared");
        return Views.savingsBatch(batch);
    }

    @Transactional
    SavingView submit(Long id) {
        SavingsTransaction transaction = require(id);
        workflow.submit(transaction, "SAVING", transaction.reference);
        return Views.saving(transaction);
    }

    @Transactional
    SavingView decide(Long id, DecisionRequest decision) {
        SavingsTransaction transaction = require(id);
        if (decision.approve() && transaction.savingType == SavingType.WITHDRAWAL) {
            validateWithdrawal(transaction.member.id, transaction.amount);
        }
        workflow.decide(transaction, decision.approve(), decision.remarks(),
                "SAVING", transaction.reference);
        return Views.saving(transaction);
    }

    @Transactional
    BatchView submitBatch(Long id) {
        SavingsBatch batch = requireBatch(id);
        workflow.submit(batch, "SAVINGS_BATCH", "SB-" + batch.id);
        for (SavingsTransaction transaction : savings.findByBatchIdOrderByMemberFullNameAsc(id)) {
            transaction.workflowStatus = WorkflowStatus.PENDING_APPROVAL;
            transaction.submittedBy = batch.submittedBy;
            transaction.submittedAt = batch.submittedAt;
        }
        return Views.savingsBatch(batch);
    }

    @Transactional
    BatchView decideBatch(Long id, DecisionRequest decision) {
        SavingsBatch batch = requireBatch(id);
        workflow.decide(batch, decision.approve(), decision.remarks(),
                "SAVINGS_BATCH", "SB-" + batch.id);
        for (SavingsTransaction transaction : savings.findByBatchIdOrderByMemberFullNameAsc(id)) {
            transaction.workflowStatus = batch.workflowStatus;
            transaction.actionedBy = batch.actionedBy;
            transaction.actionedAt = batch.actionedAt;
            transaction.decisionRemarks = batch.decisionRemarks;
        }
        return Views.savingsBatch(batch);
    }

    SavingsTransaction require(Long id) {
        return savings.findById(id).orElseThrow(() -> new NotFoundException("Savings transaction not found"));
    }

    SavingsBatch requireBatch(Long id) {
        return batches.findById(id).orElseThrow(() -> new NotFoundException("Savings batch not found"));
    }

    private void validateWithdrawal(Long memberId, BigDecimal withdrawal) {
        BigDecimal available = members.savingBalance(memberId);
        BigDecimal minimum = settings.decimal("minimumSavingsBalance", BigDecimal.ZERO);
        if (available.subtract(withdrawal).compareTo(minimum) < 0) {
            throw new BusinessException("Withdrawal exceeds available savings. Available amount is " + available);
        }
    }
}

