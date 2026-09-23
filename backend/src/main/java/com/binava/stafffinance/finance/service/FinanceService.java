package com.binava.stafffinance.finance.service;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.approval.service.WorkflowService;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.common.References;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.finance.dto.FinanceRequest;
import com.binava.stafffinance.finance.dto.FinanceView;
import com.binava.stafffinance.finance.entity.FinanceCategory;
import com.binava.stafffinance.finance.entity.FinanceTransaction;
import com.binava.stafffinance.finance.entity.FinanceType;
import com.binava.stafffinance.finance.mapper.FinanceMapper;
import com.binava.stafffinance.finance.repository.FinanceCategoryRepository;
import com.binava.stafffinance.finance.repository.FinanceRepository;
import com.binava.stafffinance.user.entity.AppUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {
    private final FinanceRepository finances;
    private final FinanceCategoryRepository categories;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;

    public FinanceService(FinanceRepository finances, FinanceCategoryRepository categories,
                   AuthService auth, WorkflowService workflow, AuditService audit) {
        this.finances = finances;
        this.categories = categories;
        this.auth = auth;
        this.workflow = workflow;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<FinanceView> list(FinanceType type, WorkflowStatus status) {
        return finances.findAllByOrderByTransactionDateDescCreatedAtDesc().stream()
                .filter(item -> type == null || item.financeType == type)
                .filter(item -> status == null || item.workflowStatus == status)
                .map(FinanceMapper::finance).toList();
    }

    @Transactional
    public FinanceView create(FinanceRequest request) {
        FinanceCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Income or expense category not found"));
        if (!category.active || !category.categoryType.name().equals(request.financeType().name())) {
            throw new BusinessException("The selected category does not match the transaction type");
        }
        AppUser user = auth.currentUser();
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.financeType = request.financeType();
        transaction.category = category;
        transaction.transactionDate = request.transactionDate();
        transaction.description = request.description().trim();
        transaction.amount = Money.amount(request.amount());
        transaction.reference = request.reference() == null || request.reference().isBlank()
                ? References.next(request.financeType() == FinanceType.INCOME ? "INC" : "EXP")
                : request.reference().trim();
        transaction.supportingReference = request.supportingReference();
        transaction.createdBy = user;
        finances.save(transaction);
        audit.log(user, "CREATE", "FINANCE", transaction.id, transaction.reference,
                null, transaction.workflowStatus, request.financeType() + " transaction created");
        return FinanceMapper.finance(transaction);
    }

    @Transactional
    public FinanceView submit(Long id) {
        FinanceTransaction transaction = require(id);
        workflow.submit(transaction, "FINANCE", transaction.reference);
        return FinanceMapper.finance(transaction);
    }

    @Transactional
    public FinanceView decide(Long id, DecisionRequest request) {
        FinanceTransaction transaction = require(id);
        workflow.decide(transaction, request.approve(), request.remarks(),
                "FINANCE", transaction.reference);
        return FinanceMapper.finance(transaction);
    }

    public FinanceTransaction require(Long id) {
        return finances.findById(id)
                .orElseThrow(() -> new NotFoundException("Income or expense transaction not found"));
    }
}
