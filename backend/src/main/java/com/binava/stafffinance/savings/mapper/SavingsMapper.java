package com.binava.stafffinance.savings.mapper;

import com.binava.stafffinance.common.dto.BatchView;
import com.binava.stafffinance.savings.dto.SavingView;
import com.binava.stafffinance.savings.entity.SavingsBatch;
import com.binava.stafffinance.savings.entity.SavingsTransaction;

public final class SavingsMapper {
    private SavingsMapper() {}

    public static SavingView saving(SavingsTransaction saving) {
        return new SavingView(saving.id, saving.member.id, saving.member.fullName, saving.savingType,
                saving.amount, saving.transactionDate, saving.reference, saving.description,
                saving.workflowStatus, saving.createdBy.fullName,
                saving.actionedBy == null ? null : saving.actionedBy.fullName, saving.createdAt,
                saving.batch == null ? null : saving.batch.id);
    }

    public static BatchView savingsBatch(SavingsBatch batch) {
        return new BatchView(batch.id, batch.period, batch.totalAmount, batch.workflowStatus,
                batch.createdBy.fullName, batch.actionedBy == null ? null : batch.actionedBy.fullName,
                batch.createdAt, batch.remarks);
    }
}
