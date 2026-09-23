package com.binava.stafffinance.savings.service;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.approval.service.WorkflowService;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.common.References;
import com.binava.stafffinance.common.dto.BatchView;
import com.binava.stafffinance.config.service.SettingService;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.service.MemberService;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.savings.dto.SavingRequest;
import com.binava.stafffinance.savings.dto.SavingView;
import com.binava.stafffinance.savings.dto.SavingsBatchItemRequest;
import com.binava.stafffinance.savings.dto.SavingsBatchRequest;
import com.binava.stafffinance.savings.entity.SavingType;
import com.binava.stafffinance.savings.entity.SavingsBatch;
import com.binava.stafffinance.savings.entity.SavingsTransaction;
import com.binava.stafffinance.savings.mapper.SavingsMapper;
import com.binava.stafffinance.savings.repository.SavingsBatchRepository;
import com.binava.stafffinance.savings.repository.SavingsRepository;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsService {
    private final SavingsRepository savings;
    private final SavingsBatchRepository batches;
    private final MemberService members;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;
    private final SettingService settings;

    public SavingsService(SavingsRepository savings, SavingsBatchRepository batches, MemberService members,
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
    public List<SavingView> list(Long memberId, SavingType type, WorkflowStatus status) {
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
                .map(SavingsMapper::saving).toList();
    }

    @Transactional(readOnly = true)
    public List<BatchView> listBatches() {
        return batches.findAllByOrderByCreatedAtDesc().stream().map(SavingsMapper::savingsBatch).toList();
    }

    @Transactional(readOnly = true)
    public List<SavingView> batchItems(Long batchId) {
        requireBatch(batchId);
        return savings.findByBatchIdOrderByMemberFullNameAsc(batchId).stream().map(SavingsMapper::saving).toList();
    }

    @Transactional
    public SavingView create(SavingRequest request, SavingType type) {
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
        return SavingsMapper.saving(transaction);
    }

    @Transactional
    public BatchView createBatch(SavingsBatchRequest request) {
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
        return SavingsMapper.savingsBatch(batch);
    }

    @Transactional
    public SavingView submit(Long id) {
        SavingsTransaction transaction = require(id);
        assertStandalone(transaction);
        workflow.submit(transaction, "SAVING", transaction.reference);
        return SavingsMapper.saving(transaction);
    }

    @Transactional
    public SavingView decide(Long id, DecisionRequest decision) {
        SavingsTransaction transaction = require(id);
        assertStandalone(transaction);
        if (decision.approve() && transaction.savingType == SavingType.WITHDRAWAL) {
            validateWithdrawal(transaction.member.id, transaction.amount);
        }
        workflow.decide(transaction, decision.approve(), decision.remarks(),
                "SAVING", transaction.reference);
        return SavingsMapper.saving(transaction);
    }

    @Transactional
    public BatchView submitBatch(Long id) {
        SavingsBatch batch = requireBatch(id);
        workflow.submit(batch, "SAVINGS_BATCH", "SB-" + batch.id);
        for (SavingsTransaction transaction : savings.findByBatchIdOrderByMemberFullNameAsc(id)) {
            transaction.workflowStatus = WorkflowStatus.PENDING_APPROVAL;
            transaction.submittedBy = batch.submittedBy;
            transaction.submittedAt = batch.submittedAt;
        }
        return SavingsMapper.savingsBatch(batch);
    }

    @Transactional
    public BatchView decideBatch(Long id, DecisionRequest decision) {
        SavingsBatch batch = requireBatch(id);
        workflow.decide(batch, decision.approve(), decision.remarks(),
                "SAVINGS_BATCH", "SB-" + batch.id);
        for (SavingsTransaction transaction : savings.findByBatchIdOrderByMemberFullNameAsc(id)) {
            transaction.workflowStatus = batch.workflowStatus;
            transaction.actionedBy = batch.actionedBy;
            transaction.actionedAt = batch.actionedAt;
            transaction.decisionRemarks = batch.decisionRemarks;
        }
        return SavingsMapper.savingsBatch(batch);
    }

    public SavingsTransaction require(Long id) {
        return savings.findById(id).orElseThrow(() -> new NotFoundException("Savings transaction not found"));
    }

    public SavingsBatch requireBatch(Long id) {
        return batches.findById(id).orElseThrow(() -> new NotFoundException("Savings batch not found"));
    }

    private void validateWithdrawal(Long memberId, BigDecimal withdrawal) {
        BigDecimal available = members.savingBalance(memberId);
        BigDecimal minimum = settings.decimal("minimumSavingsBalance", BigDecimal.ZERO);
        if (available.subtract(withdrawal).compareTo(minimum) < 0) {
            throw new BusinessException("Withdrawal exceeds available savings. Available amount is " + available);
        }
    }

    private void assertStandalone(SavingsTransaction transaction) {
        if (transaction.batch != null) {
            throw new BusinessException("Submit or decide this saving through its monthly batch");
        }
    }
}
