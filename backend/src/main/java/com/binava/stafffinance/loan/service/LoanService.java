package com.binava.stafffinance.loan.service;

import com.binava.stafffinance.approval.dto.DecisionRequest;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.approval.service.WorkflowService;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.common.References;
import com.binava.stafffinance.config.service.SettingService;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.exception.NotFoundException;

import com.binava.stafffinance.loan.dto.LoanDecisionRequest;
import com.binava.stafffinance.loan.dto.LoanRequest;
import com.binava.stafffinance.loan.dto.LoanView;
import com.binava.stafffinance.loan.dto.ScheduleView;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanSchedule;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.mapper.LoanMapper;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.loan.repository.LoanScheduleRepository;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.service.MemberService;
import com.binava.stafffinance.repayment.entity.PaymentStatus;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.user.entity.AppUser;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Autowired private com.binava.stafffinance.loan.repository.LoanCategoryRepository categories;
    @org.springframework.beans.factory.annotation.Autowired private InstallmentScheduleService scheduleService;
    @org.springframework.beans.factory.annotation.Autowired private LoanCalculationService calculator;
    private final LoanRepository loans;
    private final LoanScheduleRepository schedules;
    private final RepaymentRepository repayments;
    private final MemberService members;
    private final AuthService auth;
    private final WorkflowService workflow;
    private final AuditService audit;
    private final SettingService settings;

    public LoanService(LoanRepository loans, LoanScheduleRepository schedules,
                RepaymentRepository repayments,
                MemberService members, AuthService auth, WorkflowService workflow,
                AuditService audit, SettingService settings) {
        this.loans = loans;
        this.schedules = schedules;
        this.repayments = repayments;
        this.members = members;
        this.auth = auth;
        this.workflow = workflow;
        this.audit = audit;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public List<LoanView> list(Long memberId, LoanStatus status) {
        AppUser current = auth.currentUser();
        Long effectiveMemberId = memberId;
        if (current.role == Role.MEMBER) {
            if (current.member == null) throw new BusinessException("This account is not linked to a member");
            effectiveMemberId = current.member.id;
        }
        List<Loan> source = effectiveMemberId == null
                ? loans.findAllByOrderByApplicationDateDescCreatedAtDesc()
                : loans.findByMemberIdOrderByApplicationDateDesc(effectiveMemberId);
        return source.stream()
                .filter(loan -> status == null || loan.loanStatus == status)
                .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public LoanView get(Long id) {
        Loan loan = requireLoan(id);
        assertMemberAccess(loan.member.id);
        return view(loan);
    }

    @Transactional(readOnly = true)
    public List<ScheduleView> schedule(Long loanId) {
        Loan loan = requireLoan(loanId);
        assertMemberAccess(loan.member.id);
        return scheduleService.active(loan)
                .stream().map(LoanMapper::schedule).toList();
    }

    @Transactional
    public LoanView create(LoanRequest request) {
        Member member = members.require(request.memberId());
        lockMember(member);
        if (member.membershipStatus != MembershipStatus.ACTIVE &&
                !settings.bool("allowLoansForLeftMembers", false)) {
            throw new BusinessException("Only active members may receive a new loan");
        }
        boolean hasActiveLoan = loans.findByMemberIdOrderByApplicationDateDesc(member.id).stream()
                .anyMatch(loan -> loan.loanStatus == LoanStatus.ACTIVE ||
                        loan.loanStatus == LoanStatus.PENDING_APPROVAL || loan.loanStatus == LoanStatus.APPROVED);
        if (hasActiveLoan) throw new BusinessException("This staff member already has an active loan. Complete, Top Up, Reschedule, or Restructure the existing loan instead.");

        AppUser user = auth.currentUser();
        Loan loan = new Loan();
        loan.applicationNumber = References.next("LOAN");
        loan.member = member;
        loan.applicationDate = request.applicationDate();
        loan.interestMethod = "REDUCING_BALANCE";
        loan.requestedAmount = Money.amount(request.requestedAmount());
        loan.annualInterestRate = request.annualInterestRate();
        loan.repaymentMonths = request.repaymentMonths();
        loan.purpose = request.purpose().trim();
        loan.remarks = request.remarks();
        loan.createdBy = user;
        if(request.firstInstallmentDate().isBefore(request.applicationDate())) throw new BusinessException("First installment date cannot precede the application date");
        loan.firstInstallmentDate=request.firstInstallmentDate(); loan.originalTerm=request.repaymentMonths(); loan.originalPrincipal=loan.requestedAmount;
        loans.save(loan);
        var version=scheduleService.propose(loan,loan.requestedAmount,loan.repaymentMonths,loan.firstInstallmentDate,"ORIGINAL",loan.applicationDate,BigDecimal.ZERO,loan.remarks,user);
        loan.currentScheduleVersionId=version.id; loan.activePrincipal=version.principal;
        loan.totalInterest=version.interest; loan.totalPayable=version.totalPayable; loan.monthlyInstallment=version.installment;
        audit.log(user, "CREATE", "LOAN", loan.id, loan.applicationNumber,
                null, loan.loanStatus, "Loan application created for " + member.fullName);
        return view(loan);
    }

    @Transactional
    public LoanView submit(Long id) {
        Loan loan = requireLoan(id);
        lockMember(loan.member);
        boolean hasOtherLoan = loans.findByMemberIdOrderByApplicationDateDesc(loan.member.id).stream()
                .anyMatch(other -> !other.id.equals(loan.id) &&
                        (other.loanStatus == LoanStatus.ACTIVE ||
                         other.loanStatus == LoanStatus.PENDING_APPROVAL ||
                         other.loanStatus == LoanStatus.APPROVED));
        if (hasOtherLoan) throw new BusinessException("This staff member already has an active loan. Complete, Top Up, Reschedule, or Restructure the existing loan instead.");
        workflow.submit(loan, "LOAN", loan.applicationNumber);
        loan.committedMemberId = loan.member.id;
        loan.loanStatus = LoanStatus.PENDING_APPROVAL;
        if(loan.currentScheduleVersionId!=null) {
            var version=scheduleService.requireVersion(loan.currentScheduleVersionId);
            version.workflowStatus=WorkflowStatus.PENDING_APPROVAL; version.submittedBy=loan.submittedBy; version.submittedAt=loan.submittedAt;
        }
        return view(loan);
    }

    @Transactional
    public LoanView approve(Long id, LoanDecisionRequest request) {
        Loan loan = requireLoan(id);
        lockMember(loan.member);
        assertNoOtherLoan(loan);
        workflow.decide(loan, true, request.remarks(), "LOAN", loan.applicationNumber);
        BigDecimal principal = Money.amount(request.approvedAmount() == null
                ? loan.requestedAmount : request.approvedAmount());
        BigDecimal rate = request.annualInterestRate() == null
                ? loan.annualInterestRate : request.annualInterestRate();
        int months = request.repaymentMonths() == null ? loan.repaymentMonths : request.repaymentMonths();
        if (principal.compareTo(loan.requestedAmount) > 0) {
            throw new BusinessException("Approved amount cannot exceed the requested amount");
        }
        if (principal.compareTo(BigDecimal.ONE) < 0 || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0 || months < 1 || months > 120) {
            throw new BusinessException("Loan terms are invalid");
        }

        LoanCalculator.Breakdown terms = LoanCalculator.reducingBalance(principal, rate, months);
        loan.interestMethod = "REDUCING_BALANCE";
        loan.approvedAmount = principal;
        loan.annualInterestRate = rate;
        loan.repaymentMonths = months;
        loan.totalInterest = terms.interest();
        loan.totalPayable = terms.totalPayable();
        loan.monthlyInstallment = terms.monthlyInstallment();
        loan.outstandingBalance = terms.totalPayable();
        loan.committedMemberId = loan.member.id;
        loan.loanStatus = LoanStatus.ACTIVE;
        // Retain the legacy date field as the activation date for repayment validation.
        loan.disbursementDate = java.time.LocalDate.now();
        loan.disbursementReference = "APPROVAL-" + loan.applicationNumber;
        loan.originalPrincipal=principal; loan.originalTerm=months;
        var current=loan.currentScheduleVersionId==null?null:scheduleService.requireVersion(loan.currentScheduleVersionId);
        if(current==null || current.principal.compareTo(principal)!=0 || current.annualRate.compareTo(rate)!=0 || current.term!=months) {
            var version=scheduleService.propose(loan,principal,months,loan.firstInstallmentDate==null?loan.applicationDate.plusMonths(1):loan.firstInstallmentDate,"APPROVED_TERMS",loan.applicationDate,BigDecimal.ZERO,request.remarks(),loan.createdBy);
            scheduleService.activate(loan,version,auth.currentUser(),request.remarks());
        } else {
            current.workflowStatus=WorkflowStatus.APPROVED; current.actionedBy=auth.currentUser(); current.actionedAt=java.time.Instant.now(); current.decisionRemarks=request.remarks();
        }
        return view(loan);
    }

    @Transactional
    public LoanView reject(Long id, DecisionRequest request) {
        Loan loan = requireLoan(id);
        workflow.decide(loan, false, request.remarks(), "LOAN", loan.applicationNumber);
        loan.committedMemberId = null;
        loan.loanStatus = LoanStatus.REJECTED;
        if(loan.currentScheduleVersionId!=null) { var version=scheduleService.requireVersion(loan.currentScheduleVersionId); version.workflowStatus=WorkflowStatus.REJECTED; version.actionedBy=loan.actionedBy; version.actionedAt=loan.actionedAt; version.decisionRemarks=request.remarks(); }
        return view(loan);
    }

    public Loan requireLoan(Long id) {
        return loans.findById(id).orElseThrow(() -> new NotFoundException("Loan not found"));
    }

    private void lockMember(Member member) {
        if (entityManager != null) entityManager.lock(member, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }
    private void assertNoOtherLoan(Loan loan) {
        if (loans.findByMemberIdOrderByApplicationDateDesc(loan.member.id).stream().anyMatch(other ->
                !other.id.equals(loan.id) && (other.loanStatus == LoanStatus.ACTIVE || other.loanStatus == LoanStatus.APPROVED || other.loanStatus == LoanStatus.PENDING_APPROVAL)))
            throw new BusinessException("This staff member already has an active loan. Complete, Top Up, Reschedule, or Restructure the existing loan instead.");
    }
    @Transactional
    public LoanView createCategorized(LoanRequest request, Long categoryId) {
        var category = categories.findById(categoryId).orElseThrow(() -> new NotFoundException("Loan category not found"));
        if (!category.active) throw new BusinessException("Select an active loan category");
        if (category.annualRate.compareTo(request.annualInterestRate()) != 0)
            throw new BusinessException("The category rate has changed. Refresh and review the schedule again");
        LoanView created = create(new LoanRequest(request.memberId(), request.applicationDate(), request.requestedAmount(), category.annualRate, request.repaymentMonths(), request.purpose(), request.remarks(), request.firstInstallmentDate()));
        Loan loan = requireLoan(created.id()); loan.categoryId=category.id; loan.categoryName = category.name; return view(loan);
    }
    @Transactional(readOnly=true)
    public LoanCalculationService.Calculation preview(com.binava.stafffinance.loan.dto.LoanRequest request, Long categoryId) {
        BigDecimal rate=request.annualInterestRate();
        if(categoryId!=null) { var category=categories.findById(categoryId).orElseThrow(()->new NotFoundException("Loan category not found")); if(!category.active) throw new BusinessException("Select an active loan category"); rate=category.annualRate; }
        if(request.firstInstallmentDate().isBefore(request.applicationDate())) throw new BusinessException("First installment date cannot precede the application date");
        return calculator.calculate(request.requestedAmount(),rate,request.repaymentMonths(),request.firstInstallmentDate());
    }

    @Transactional
    public List<com.binava.stafffinance.loan.dto.ScheduleVersionView> scheduleHistory(Long id) {
        Loan loan=requireLoan(id); assertMemberAccess(loan.member.id); lockMember(loan.member);
        scheduleService.ensureBaseline(loan); return scheduleService.history(loan);
    }

    @Transactional(readOnly=true)
    public LoanCalculationService.Calculation adjustmentPreview(Long id, com.binava.stafffinance.loan.dto.AdjustmentRequest request) {
        Loan loan=requireLoan(id); assertMemberAccess(loan.member.id); return adjustmentTerms(loan,request);
    }

    private LoanCalculationService.Calculation adjustmentTerms(Loan loan, com.binava.stafffinance.loan.dto.AdjustmentRequest request) {
        if(loan.loanStatus!=LoanStatus.ACTIVE) throw new BusinessException("Only active loans can be changed");
        if(!List.of("TOP_UP","RESCHEDULE","RESTRUCTURE").contains(request.type())) throw new BusinessException("Invalid loan change");
        var rows=scheduleService.active(loan);
        if(rows.stream().anyMatch(r->r.principalAmount==null)) throw new BusinessException("Legacy schedule needs principal/interest allocation before changing its terms");
        int remaining=scheduleService.remainingCount(loan);
        if(request.effectiveDate().isBefore(loan.disbursementDate) || request.effectiveDate().isAfter(java.time.LocalDate.now())) throw new BusinessException("Effective date must be between activation and today");
        if(repayments.findByLoanIdOrderByPaymentDateDesc(loan.id).stream().anyMatch(p->p.workflowStatus==WorkflowStatus.PENDING_APPROVAL || p.workflowStatus==WorkflowStatus.DRAFT || (p.workflowStatus==WorkflowStatus.APPROVED && p.paymentDate.isAfter(request.effectiveDate()))))
            throw new BusinessException("Resolve draft/pending repayments and use an effective date after approved payments");
        if((request.type().equals("RESCHEDULE") && request.amount().signum()!=0) || (!request.type().equals("RESCHEDULE") && request.amount().compareTo(new BigDecimal("0.01"))<0)) throw new BusinessException("Invalid additional principal");
        if(!request.type().equals("TOP_UP") && request.months()==remaining) throw new BusinessException("Select a different remaining term");
        for(var row:rows) {
            if(row.dueDate.isBefore(request.effectiveDate()) && scheduleService.remaining(row).signum()>0) throw new BusinessException("Pay overdue installments before changing terms");
            if(row.amountPaid.signum()>0 && row.amountPaid.compareTo(row.interestAmount)<0) throw new BusinessException("Complete partially paid interest before changing terms");
        }
        return calculator.calculate(scheduleService.remainingPrincipal(loan).add(request.amount()),loan.annualInterestRate,request.months(),request.effectiveDate().plusMonths(1));
    }

    @Transactional
    public LoanView requestAdjustment(Long id, com.binava.stafffinance.loan.dto.AdjustmentRequest request) {
        Loan loan=requireLoan(id); lockMember(loan.member);
        if(loan.adjustmentType!=null) throw new BusinessException("A loan change is already awaiting approval");
        var terms=adjustmentTerms(loan,request); var user=auth.currentUser();
        var version=scheduleService.propose(loan,terms.principal(),request.months(),request.effectiveDate().plusMonths(1),request.type(),request.effectiveDate(),Money.amount(request.amount()),request.remarks(),user);
        version.workflowStatus=WorkflowStatus.PENDING_APPROVAL; version.submittedBy=user; version.submittedAt=java.time.Instant.now();
        loan.pendingScheduleVersionId=version.id; loan.adjustmentType=request.type(); loan.adjustmentAmount=Money.amount(request.amount()); loan.adjustmentMonths=request.months();
        loan.adjustmentDate=request.effectiveDate(); loan.adjustmentRemarks=request.remarks(); loan.adjustmentRequestedBy=user.id; loan.adjustmentRequestedName=user.fullName;
        audit.log(user,"SUBMIT_CHANGE","LOAN",loan.id,loan.applicationNumber,null,request.type(),"Schedule version "+version.versionNumber+": "+request.remarks());
        return view(loan);
    }

    @Transactional
    public LoanView decideAdjustment(Long id, DecisionRequest decision) {
        Loan loan=requireLoan(id); lockMember(loan.member);
        if(loan.adjustmentType==null) throw new BusinessException("No pending loan change");
        var user=auth.currentUser();
        if(user.id.equals(loan.adjustmentRequestedBy)) throw new BusinessException("You cannot approve your own loan change");
        if(!decision.approve() && (decision.remarks()==null || decision.remarks().isBlank())) throw new BusinessException("Rejection remarks are required");
        var request=new com.binava.stafffinance.loan.dto.AdjustmentRequest(loan.adjustmentType,loan.adjustmentAmount,loan.adjustmentMonths,loan.adjustmentDate,loan.adjustmentRemarks);
        com.binava.stafffinance.loan.entity.LoanScheduleVersion version=loan.pendingScheduleVersionId==null?null:scheduleService.requireVersion(loan.pendingScheduleVersionId);
        if(decision.approve()) {
            var terms=adjustmentTerms(loan,request);
            if(version==null) version=scheduleService.propose(loan,terms.principal(),request.months(),request.effectiveDate().plusMonths(1),request.type(),request.effectiveDate(),request.amount(),request.remarks(),loan.createdBy);
            if(version.principal.compareTo(terms.principal())!=0 || version.totalPayable.compareTo(terms.totalPayable())!=0) throw new BusinessException("Loan balances changed; reject and submit a new proposal");
            loan.approvedAmount=loan.approvedAmount.add(loan.adjustmentAmount);
            scheduleService.activate(loan,version,user,decision.remarks());
        } else if(version!=null) { version.workflowStatus=WorkflowStatus.REJECTED; version.actionedBy=user; version.actionedAt=java.time.Instant.now(); version.decisionRemarks=decision.remarks(); }
        audit.log(user,decision.approve()?"APPROVE_CHANGE":"REJECT_CHANGE","LOAN",loan.id,loan.applicationNumber,loan.adjustmentType,loan.loanStatus,
            "Additional principal="+loan.adjustmentAmount+", term="+loan.adjustmentMonths+", effective="+loan.adjustmentDate+", remarks="+decision.remarks());
        loan.adjustmentType=null; loan.pendingScheduleVersionId=null; loan.adjustmentAmount=null; loan.adjustmentMonths=null; loan.adjustmentDate=null; loan.adjustmentRequestedBy=null; loan.adjustmentRequestedName=null;
        return view(loan);
    }

    private LoanView view(Loan loan) {
        var approved=repayments.findByLoanIdOrderByPaymentDateDesc(loan.id).stream().filter(p->p.workflowStatus==WorkflowStatus.APPROVED).toList();
        BigDecimal repaid=approved.stream().map(p->p.amount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal charges=approved.stream().map(p->Money.amount(p.chargesPaid)).reduce(BigDecimal.ZERO,BigDecimal::add);
        return LoanMapper.loan(loan,repaid,scheduleService.interestPaid(loan),scheduleService.paidCount(loan),scheduleService.remainingCount(loan),charges);
    }

    private void assertMemberAccess(Long memberId) {
        AppUser user = auth.currentUser();
        if (user.role == Role.MEMBER && (user.member == null || !user.member.id.equals(memberId))) {
            throw new org.springframework.security.access.AccessDeniedException("Members can only view their own loans");
        }
    }
}
