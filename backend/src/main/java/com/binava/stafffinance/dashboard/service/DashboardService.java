package com.binava.stafffinance.dashboard.service;

import com.binava.stafffinance.approval.dto.ApprovalView;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.approval.service.ApprovalService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.dashboard.dto.DashboardView;
import com.binava.stafffinance.finance.entity.FinanceTransaction;
import com.binava.stafffinance.finance.entity.FinanceType;
import com.binava.stafffinance.finance.repository.FinanceRepository;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.repository.MemberRepository;
import com.binava.stafffinance.repayment.entity.LoanRepayment;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.savings.entity.SavingType;
import com.binava.stafffinance.savings.entity.SavingsTransaction;
import com.binava.stafffinance.savings.repository.SavingsRepository;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final MemberRepository members;
    private final SavingsRepository savings;
    private final LoanRepository loans;
    private final RepaymentRepository repayments;
    private final FinanceRepository finances;
    private final ApprovalService approvals;
    private final AuthService auth;

    public DashboardService(MemberRepository members, SavingsRepository savings, LoanRepository loans,
                     RepaymentRepository repayments, FinanceRepository finances,
                     ApprovalService approvals, AuthService auth) {
        this.members = members;
        this.savings = savings;
        this.loans = loans;
        this.repayments = repayments;
        this.finances = finances;
        this.approvals = approvals;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard() {
        AppUser user = auth.currentUser();
        Long memberId = user.role == Role.MEMBER && user.member != null ? user.member.id : null;
        boolean personal = user.role == Role.MEMBER;

        List<Member> memberList = personal
                ? (user.member == null ? List.of() : List.of(user.member))
                : members.findAll();
        List<SavingsTransaction> savingList = personal && memberId != null
                ? savings.findByMemberIdOrderByTransactionDateDesc(memberId)
                : savings.findAll();
        List<Loan> loanList = personal && memberId != null
                ? loans.findByMemberIdOrderByApplicationDateDesc(memberId)
                : loans.findAll();
        List<LoanRepayment> repaymentList = personal
                ? repayments.findAll().stream().filter(item -> memberId != null && item.loan.member.id.equals(memberId)).toList()
                : repayments.findAll();

        BigDecimal totalSavings = savingList.stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                .map(item -> item.savingType == SavingType.WITHDRAWAL ? item.amount.negate() : item.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Loan> active = loanList.stream().filter(item -> item.loanStatus == LoanStatus.ACTIVE).toList();
        BigDecimal outstanding = active.stream().map(item -> item.outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal repaid = repaymentList.stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal income = personal ? BigDecimal.ZERO : financeTotal(FinanceType.INCOME);
        BigDecimal expenses = personal ? BigDecimal.ZERO : financeTotal(FinanceType.EXPENSE);
        long withOutstanding = loanList.stream()
                .filter(item -> item.loanStatus == LoanStatus.ACTIVE && item.outstandingBalance.signum() > 0)
                .map(item -> item.member.id).distinct().count();
        long leftWithOutstanding = loanList.stream()
                .filter(item -> item.loanStatus == LoanStatus.ACTIVE && item.outstandingBalance.signum() > 0)
                .filter(item -> item.member.membershipStatus == MembershipStatus.LEFT)
                .map(item -> item.member.id).distinct().count();
        List<ApprovalView> pending = personal ? List.of() : approvals.pending();

        return new DashboardView(memberList.size(),
                memberList.stream().filter(item -> item.membershipStatus == MembershipStatus.ACTIVE).count(),
                Money.amount(totalSavings), active.size(), Money.amount(outstanding), Money.amount(repaid),
                Money.amount(income), Money.amount(expenses), withOutstanding, leftWithOutstanding,
                pending.size(), trend(savingList, repaymentList, personal), pending.stream().limit(5).toList());
    }

    private BigDecimal financeTotal(FinanceType type) {
        return finances.findAll().stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED && item.financeType == type)
                .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> trend(List<SavingsTransaction> savingList,
                                            List<LoanRepayment> repaymentList, boolean personal) {
        List<FinanceTransaction> financeList = personal ? List.of() : finances.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = current.minusMonths(offset);
            BigDecimal saved = savingList.stream()
                    .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                    .filter(item -> YearMonth.from(item.transactionDate).equals(month))
                    .map(item -> item.savingType == SavingType.WITHDRAWAL ? item.amount.negate() : item.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal repaid = repaymentList.stream()
                    .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                    .filter(item -> YearMonth.from(item.paymentDate).equals(month))
                    .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal income = financeList.stream()
                    .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED && item.financeType == FinanceType.INCOME)
                    .filter(item -> YearMonth.from(item.transactionDate).equals(month))
                    .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = financeList.stream()
                    .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED && item.financeType == FinanceType.EXPENSE)
                    .filter(item -> YearMonth.from(item.transactionDate).equals(month))
                    .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.toString());
            row.put("savings", Money.amount(saved));
            row.put("repayments", Money.amount(repaid));
            row.put("income", Money.amount(income));
            row.put("expenses", Money.amount(expense));
            rows.add(row);
        }
        return rows;
    }
}
