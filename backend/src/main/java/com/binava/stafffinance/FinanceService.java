package com.binava.stafffinance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class FinanceService {
    private final FinanceRepository finances;
    private final FinanceCategoryRepository categories;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;

    FinanceService(FinanceRepository finances, FinanceCategoryRepository categories,
                   AuthService auth, WorkflowService workflow, AuditService audit) {
        this.finances = finances;
        this.categories = categories;
        this.auth = auth;
        this.workflow = workflow;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    List<FinanceView> list(FinanceType type, WorkflowStatus status) {
        return finances.findAllByOrderByTransactionDateDescCreatedAtDesc().stream()
                .filter(item -> type == null || item.financeType == type)
                .filter(item -> status == null || item.workflowStatus == status)
                .map(Views::finance).toList();
    }

    @Transactional
    FinanceView create(FinanceRequest request) {
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
        return Views.finance(transaction);
    }

    @Transactional
    FinanceView submit(Long id) {
        FinanceTransaction transaction = require(id);
        workflow.submit(transaction, "FINANCE", transaction.reference);
        return Views.finance(transaction);
    }

    @Transactional
    FinanceView decide(Long id, DecisionRequest request) {
        FinanceTransaction transaction = require(id);
        workflow.decide(transaction, request.approve(), request.remarks(),
                "FINANCE", transaction.reference);
        return Views.finance(transaction);
    }

    FinanceTransaction require(Long id) {
        return finances.findById(id)
                .orElseThrow(() -> new NotFoundException("Income or expense transaction not found"));
    }
}

