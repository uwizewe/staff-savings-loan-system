package com.binava.stafffinance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
class MemberService {
    private final MemberRepository members;
    private final SavingsRepository savings;
    private final LoanRepository loans;
    private final RepaymentRepository repayments;
    private final AuthService auth;
    private final AuditService audit;

    MemberService(MemberRepository members, SavingsRepository savings, LoanRepository loans,
                  RepaymentRepository repayments, AuthService auth, AuditService audit) {
        this.members = members;
        this.savings = savings;
        this.loans = loans;
        this.repayments = repayments;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    List<MemberView> list(String query, MembershipStatus status, RiskStatus risk) {
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
    MemberView get(Long id) {
        return view(require(id));
    }

    @Transactional
    MemberView create(MemberRequest request) {
        if (members.existsByMemberCodeIgnoreCase(request.memberCode().trim())) {
            throw new BusinessException("Member ID is already in use");
        }
        Member member = new Member();
        apply(member, request);
        members.save(member);
        AppUser user = auth.currentUser();
        audit.log(user, "CREATE", "MEMBER", member.id, member.memberCode,
                null, member.membershipStatus, "Member registered: " + member.fullName);
        return view(member);
    }

    @Transactional
    MemberView update(Long id, MemberRequest request) {
        Member member = require(id);
        members.findByMemberCodeIgnoreCase(request.memberCode().trim())
                .filter(existing -> !existing.id.equals(id))
                .ifPresent(existing -> { throw new BusinessException("Member ID is already in use"); });
        MembershipStatus previous = member.membershipStatus;
        apply(member, request);
        AppUser user = auth.currentUser();
        audit.log(user, "UPDATE", "MEMBER", member.id, member.memberCode,
                previous, member.membershipStatus, "Member information updated");
        return view(member);
    }

    @Transactional(readOnly = true)
    MemberStatement statement(Long id) {
        Member member = require(id);
        AppUser user = auth.currentUser();
        if (user.role == Role.MEMBER && (user.member == null || !user.member.id.equals(id))) {
            throw new org.springframework.security.access.AccessDeniedException("Members can only view their own statement");
        }

        List<SavingView> savingViews = savings.findByMemberIdOrderByTransactionDateDesc(id)
                .stream().map(Views::saving).toList();
        List<Loan> memberLoans = loans.findByMemberIdOrderByApplicationDateDesc(id);
        List<LoanView> loanViews = memberLoans.stream().map(loan ->
                Views.loan(loan, approvedRepayments(loan.id))).toList();
        List<RepaymentView> repaymentViews = memberLoans.stream()
                .flatMap(loan -> repayments.findByLoanIdOrderByPaymentDateDesc(loan.id).stream())
                .map(Views::repayment).toList();
        BigDecimal totalRepaid = repaymentViews.stream()
                .filter(item -> item.status() == WorkflowStatus.APPROVED)
                .map(RepaymentView::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MemberStatement(view(member), savingViews, loanViews, repaymentViews,
                savingBalance(id), Money.amount(totalRepaid), outstandingBalance(id));
    }

    Member require(Long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("Member not found"));
    }

    BigDecimal savingBalance(Long memberId) {
        return Money.amount(savings.findByMemberIdOrderByTransactionDateDesc(memberId).stream()
                .filter(item -> item.workflowStatus == WorkflowStatus.APPROVED)
                .map(item -> item.savingType == SavingType.WITHDRAWAL
                        ? item.amount.negate() : item.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    BigDecimal outstandingBalance(Long memberId) {
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
        return new MemberView(member.id, member.memberCode, member.fullName, member.department,
                member.phone, member.email, member.monthlySavingAmount, member.membershipStatus,
                member.riskStatus, member.joiningDate, member.exitDate, member.remarks,
                savingBalance(member.id), outstandingBalance(member.id));
    }

    private void apply(Member member, MemberRequest request) {
        if (request.membershipStatus() == MembershipStatus.LEFT && request.exitDate() == null) {
            throw new BusinessException("Exit date is required when membership status is Left");
        }
        member.memberCode = request.memberCode().trim().toUpperCase(Locale.ROOT);
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

