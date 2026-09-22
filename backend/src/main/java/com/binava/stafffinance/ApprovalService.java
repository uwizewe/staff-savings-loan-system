package com.binava.stafffinance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
class ApprovalService {
    private final SavingsRepository savings;
    private final SavingsBatchRepository savingsBatches;
    private final LoanRepository loans;
    private final RepaymentRepository repayments;
    private final RepaymentBatchRepository repaymentBatches;
    private final FinanceRepository finances;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final FinanceService financeService;

    ApprovalService(SavingsRepository savings, SavingsBatchRepository savingsBatches,
                    LoanRepository loans, RepaymentRepository repayments,
                    RepaymentBatchRepository repaymentBatches, FinanceRepository finances,
                    SavingsService savingsService, LoanService loanService,
                    FinanceService financeService) {
        this.savings = savings;
        this.savingsBatches = savingsBatches;
        this.loans = loans;
        this.repayments = repayments;
        this.repaymentBatches = repaymentBatches;
        this.finances = finances;
        this.savingsService = savingsService;
        this.loanService = loanService;
        this.financeService = financeService;
    }

    @Transactional(readOnly = true)
    List<ApprovalView> pending() {
        List<ApprovalView> result = new ArrayList<>();
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
                .forEach(item -> result.add(new ApprovalView("REPAYMENT", item.id, item.reference,
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
    Object decide(String type, Long id, DecisionRequest request) {
        return switch (type.toUpperCase()) {
            case "SAVING" -> savingsService.decide(id, request);
            case "SAVINGS_BATCH" -> savingsService.decideBatch(id, request);
            case "LOAN" -> request.approve()
                    ? loanService.approve(id, new LoanDecisionRequest(request.remarks(), null, null, null))
                    : loanService.reject(id, request);
            case "REPAYMENT" -> loanService.decideRepayment(id, request);
            case "REPAYMENT_BATCH" -> loanService.decideRepaymentBatch(id, request);
            case "FINANCE" -> financeService.decide(id, request);
            default -> throw new BusinessException("Unsupported approval type: " + type);
        };
    }
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/approvals")
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
class ApprovalController {
    private final ApprovalService service;

    ApprovalController(ApprovalService service) { this.service = service; }

    @org.springframework.web.bind.annotation.GetMapping
    List<ApprovalView> pending() { return service.pending(); }

    @org.springframework.web.bind.annotation.PostMapping("/{type}/{id}/decision")
    Object decide(@org.springframework.web.bind.annotation.PathVariable String type,
                  @org.springframework.web.bind.annotation.PathVariable Long id,
                  @org.springframework.web.bind.annotation.RequestBody DecisionRequest request) {
        return service.decide(type, id, request);
    }
}

