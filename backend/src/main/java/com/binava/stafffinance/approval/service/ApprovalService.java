package com.binava.stafffinance.approval.service;

import com.binava.stafffinance.approval.dto.ApprovalView;
import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.finance.repository.FinanceRepository;
import com.binava.stafffinance.finance.service.FinanceService;
import com.binava.stafffinance.loan.dto.LoanDecisionRequest;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.loan.service.LoanService;
import com.binava.stafffinance.repayment.repository.RepaymentBatchRepository;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.repayment.service.RepaymentService;
import com.binava.stafffinance.savings.repository.SavingsBatchRepository;
import com.binava.stafffinance.savings.repository.SavingsRepository;
import com.binava.stafffinance.savings.service.SavingsService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final SavingsRepository savings;
    private final SavingsBatchRepository savingsBatches;
    private final LoanRepository loans;
    private final RepaymentRepository repayments;
    private final RepaymentBatchRepository repaymentBatches;
    private final FinanceRepository finances;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final RepaymentService repaymentService;
    private final FinanceService financeService;

    public ApprovalService(SavingsRepository savings, SavingsBatchRepository savingsBatches,
                    LoanRepository loans, RepaymentRepository repayments,
                    RepaymentBatchRepository repaymentBatches, FinanceRepository finances,
                    SavingsService savingsService, LoanService loanService,
                    FinanceService financeService, RepaymentService repaymentService) {
        this.savings = savings;
        this.savingsBatches = savingsBatches;
        this.loans = loans;
        this.repayments = repayments;
        this.repaymentBatches = repaymentBatches;
        this.finances = finances;
        this.savingsService = savingsService;
        this.loanService = loanService;
        this.repaymentService = repaymentService;
        this.financeService = financeService;
    }

    @Transactional(readOnly = true)
    public List<ApprovalView> pending() {
        List<ApprovalView> result = new ArrayList<>();
        loans.findAllByOrderByApplicationDateDescCreatedAtDesc().stream().filter(item -> item.adjustmentType != null)
                .forEach(item -> result.add(new ApprovalView("LOAN_CHANGE", item.id, item.applicationNumber,
                        item.adjustmentType + " — " + item.member.fullName, item.adjustmentAmount,
                        item.adjustmentRequestedName, item.updatedAt)));
        savings.findByWorkflowStatus(WorkflowStatus.PENDING_APPROVAL).stream()
                .filter(item -> item.batch == null)
                .forEach(item -> result.add(new ApprovalView("SAVING", item.id, item.reference,
                        item.savingType + " savings — " + item.member.fullName, item.amount,
                        item.createdBy.fullName, item.submittedAt)));
        savingsBatches.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.PENDING_APPROVAL)
                .forEach(item -> result.add(new ApprovalView("SAVINGS_BATCH", item.id, "SB-" + item.id,
                        "Monthly savings — " + item.period, item.totalAmount,
                        item.createdBy.fullName, item.submittedAt)));
        loans.findByWorkflowStatus(WorkflowStatus.PENDING_APPROVAL).forEach(item ->
                result.add(new ApprovalView("LOAN", item.id, item.applicationNumber,
                        "Loan application — " + item.member.fullName, item.requestedAmount,
                        item.createdBy.fullName, item.submittedAt)));
        repayments.findByWorkflowStatus(WorkflowStatus.PENDING_APPROVAL).stream()
                .filter(item -> item.batch == null)
                .forEach(item -> result.add(new ApprovalView("FULL_SETTLEMENT".equals(item.paymentType)?"FULL_SETTLEMENT":"REPAYMENT", item.id, item.reference,
                        "Loan repayment — " + item.loan.member.fullName, item.amount,
                        item.createdBy.fullName, item.submittedAt)));
        repaymentBatches.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.PENDING_APPROVAL)
                .forEach(item -> result.add(new ApprovalView("REPAYMENT_BATCH", item.id, "RB-" + item.id,
                        "Monthly repayments — " + item.period, item.totalAmount,
                        item.createdBy.fullName, item.submittedAt)));
        finances.findByWorkflowStatus(WorkflowStatus.PENDING_APPROVAL).forEach(item ->
                result.add(new ApprovalView("FINANCE", item.id, item.reference,
                        item.financeType + " — " + item.description, item.amount,
                        item.createdBy.fullName, item.submittedAt)));
        result.sort(Comparator.comparing(ApprovalView::submittedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Transactional
    public Object decide(String type, Long id, DecisionRequest request) {
        return switch (type.toUpperCase()) {
            case "LOAN_CHANGE" -> loanService.decideAdjustment(id, request);
            case "SAVING" -> savingsService.decide(id, request);
            case "SAVINGS_BATCH" -> savingsService.decideBatch(id, request);
            case "LOAN" -> request.approve()
                    ? loanService.approve(id, new LoanDecisionRequest(request.remarks(), null, null, null))
                    : loanService.reject(id, request);
            case "REPAYMENT", "FULL_SETTLEMENT" -> repaymentService.decideRepayment(id, request);
            case "REPAYMENT_BATCH" -> repaymentService.decideRepaymentBatch(id, request);
            case "FINANCE" -> financeService.decide(id, request);
            default -> throw new BusinessException("Unsupported approval type: " + type);
        };
    }
}
