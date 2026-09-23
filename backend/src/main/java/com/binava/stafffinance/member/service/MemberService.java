package com.binava.stafffinance.member.service;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.loan.dto.LoanView;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.mapper.LoanMapper;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.member.dto.MemberRequest;
import com.binava.stafffinance.member.dto.MemberStatement;
import com.binava.stafffinance.member.dto.MemberView;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.entity.RiskStatus;
import com.binava.stafffinance.member.mapper.MemberMapper;
import com.binava.stafffinance.member.repository.MemberRepository;
import com.binava.stafffinance.repayment.dto.RepaymentView;
import com.binava.stafffinance.repayment.mapper.RepaymentMapper;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.savings.dto.SavingView;
import com.binava.stafffinance.savings.entity.SavingType;
import com.binava.stafffinance.savings.mapper.SavingsMapper;
import com.binava.stafffinance.savings.repository.SavingsRepository;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository members;
    private final SavingsRepository savings;
    private final LoanRepository loans;
    private final RepaymentRepository repayments;
    private final AuthService auth;
    private final AuditService audit;

    public MemberService(MemberRepository members, SavingsRepository savings, LoanRepository loans,
                  RepaymentRepository repayments, AuthService auth, AuditService audit) {
        this.members = members;
        this.savings = savings;
        this.loans = loans;
        this.repayments = repayments;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<MemberView> list(String query, MembershipStatus status, RiskStatus risk) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return members.findAllByOrderByFullNameAsc().stream()
                .filter(member -> q.isEmpty() || member.fullName.toLowerCase(Locale.ROOT).contains(q)
                        || member.memberCode.toLowerCase(Locale.ROOT).contains(q)
                        || member.department.toLowerCase(Locale.ROOT).contains(q))
                .filter(member -> status == null || member.membershipStatus == status)
                .filter(member -> risk == null || member.riskStatus == risk)
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberView get(Long id) {
        return view(require(id));
    }

    @Transactional
    public MemberView create(MemberRequest request) {
        Member member = new Member();
        member.memberCode = "VFC-" + java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        apply(member, request);
        members.save(member);
        AppUser user = auth.currentUser();
        audit.log(user, "CREATE", "MEMBER", member.id, member.memberCode,
                null, member.membershipStatus, "Member registered: " + member.fullName);
        return view(member);
    }

    @Transactional
    public MemberView update(Long id, MemberRequest request) {
        Member member = require(id);
        MembershipStatus previous = member.membershipStatus;
        apply(member, request);
        AppUser user = auth.currentUser();
        audit.log(user, "UPDATE", "MEMBER", member.id, member.memberCode,
                previous, member.membershipStatus, "Member information updated");
        return view(member);
    }

    @Transactional(readOnly = true)
    public MemberStatement statement(Long id) {
        Member member = require(id);
        AppUser user = auth.currentUser();
        if (user.role == Role.MEMBER && (user.member == null || !user.member.id.equals(id))) {
            throw new org.springframework.security.access.AccessDeniedException("Members can only view their own statement");
        }

        List<SavingView> savingViews = savings.findByMemberIdOrderByTransactionDateDesc(id)
                .stream().map(SavingsMapper::saving).toList();
        List<Loan> memberLoans = loans.findByMemberIdOrderByApplicationDateDesc(id);
        List<LoanView> loanViews = memberLoans.stream().map(loan ->
                LoanMapper.loan(loan, approvedRepayments(loan.id))).toList();
        List<RepaymentView> repaymentViews = memberLoans.stream()
                .flatMap(loan -> repayments.findByLoanIdOrderByPaymentDateDesc(loan.id).stream())
                .map(RepaymentMapper::repayment).toList();
        BigDecimal totalRepaid = repaymentViews.stream()
                .filter(item -> item.status() == WorkflowStatus.APPROVED)
                .map(RepaymentView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MemberStatement(view(member), savingViews, loanViews, repaymentViews,
                savingBalance(id), Money.amount(totalRepaid), outstandingBalance(id));
    }

    public Member require(Long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("Member not found"));
    }

    public BigDecimal savingBalance(Long memberId) {
        return Money.amount(savings.findByMemberIdOrderByTransactionDateDesc(memberId).stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                .map(item -> item.savingType == SavingType.WITHDRAWAL
                        ? item.amount.negate() : item.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal outstandingBalance(Long memberId) {
        return Money.amount(loans.findByMemberIdOrderByApplicationDateDesc(memberId).stream()
                .filter(loan -> loan.loanStatus == LoanStatus.ACTIVE)
                .map(loan -> loan.outstandingBalance == null ? BigDecimal.ZERO : loan.outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal approvedRepayments(Long loanId) {
        return repayments.findByLoanIdOrderByPaymentDateDesc(loanId).stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                .map(item -> item.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MemberView view(Member member) {
        return MemberMapper.member(member, savingBalance(member.id), outstandingBalance(member.id));
    }

    private void apply(Member member, MemberRequest request) {
        if (request.membershipStatus() == MembershipStatus.LEFT && request.exitDate() == null) {
            throw new BusinessException("Exit date is required when membership status is Left");
        }
        member.fullName = request.fullName().trim();
        member.department = request.department().trim();
        member.phone = request.phone().trim();
        member.email = request.email().trim().toLowerCase(Locale.ROOT);
        member.monthlySavingAmount = Money.amount(request.monthlySavingAmount());
        member.membershipStatus = request.membershipStatus();
        member.riskStatus = request.riskStatus();
        member.joiningDate = request.joiningDate();
        member.exitDate = request.membershipStatus() == MembershipStatus.LEFT ? request.exitDate() : null;
        member.remarks = request.remarks();
    }
}
