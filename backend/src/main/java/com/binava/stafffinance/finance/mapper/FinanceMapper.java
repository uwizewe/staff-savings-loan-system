package com.binava.stafffinance.finance.mapper;

import com.binava.stafffinance.finance.dto.FinanceView;
import com.binava.stafffinance.finance.entity.FinanceTransaction;

public final class FinanceMapper {
    private FinanceMapper() {}

    public static FinanceView finance(FinanceTransaction transaction) {
        return new FinanceView(transaction.id, transaction.reference, transaction.transactionDate,
                transaction.financeType, transaction.category.id, transaction.category.name,
                transaction.description, transaction.amount, transaction.supportingReference,
                transaction.workflowStatus, transaction.createdBy.fullName,
                transaction.actionedBy == null ? null : transaction.actionedBy.fullName,
                transaction.createdAt);
    }
}
