package com.binava.stafffinance.config;

import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.common.References;
import com.binava.stafffinance.config.entity.SystemSetting;
import com.binava.stafffinance.config.repository.SettingRepository;
import com.binava.stafffinance.finance.entity.CategoryType;
import com.binava.stafffinance.finance.entity.FinanceCategory;
import com.binava.stafffinance.finance.entity.FinanceTransaction;
import com.binava.stafffinance.finance.entity.FinanceType;
import com.binava.stafffinance.finance.repository.FinanceCategoryRepository;
import com.binava.stafffinance.finance.repository.FinanceRepository;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanSchedule;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.loan.repository.LoanScheduleRepository;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.entity.MembershipStatus;
import com.binava.stafffinance.member.entity.RiskStatus;
import com.binava.stafffinance.member.repository.MemberRepository;
import com.binava.stafffinance.repayment.entity.LoanRepayment;
import com.binava.stafffinance.repayment.entity.PaymentStatus;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.savings.entity.SavingType;
import com.binava.stafffinance.savings.entity.SavingsTransaction;
import com.binava.stafffinance.savings.repository.SavingsRepository;
import com.binava.stafffinance.user.entity.AppUser;
import com.binava.stafffinance.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class DemoDataSeeder {
    private final MemberRepository members;
    private final UserRepository users;
    private final FinanceCategoryRepository categories;
    private final SettingRepository settings;
    private final SavingsRepository savings;
    private final LoanRepository loans;
    private final LoanScheduleRepository schedules;
    private final RepaymentRepository repayments;
    private final FinanceRepository finances;
    private final PasswordEncoder passwords;
    private final boolean seedDemo;

    public DemoDataSeeder(MemberRepository members, UserRepository users,
                   FinanceCategoryRepository categories, SettingRepository settings,
                   SavingsRepository savings, LoanRepository loans,
                   LoanScheduleRepository schedules, RepaymentRepository repayments,
                   FinanceRepository finances, PasswordEncoder passwords,
                   @Value("${app.seed-demo:true}") boolean seedDemo) {
        this.members = members;
        this.users = users;
        this.categories = categories;
        this.settings = settings;
        this.savings = savings;
        this.loans = loans;
        this.schedules = schedules;
        this.repayments = repayments;
        this.finances = finances;
        this.passwords = passwords;
        this.seedDemo = seedDemo;
    }

    @Transactional
    public void seed() {
        if (!seedDemo || users.count() > 0) return;

        Member alice = member("STF-001", "Alice Mukamana", "Finance", "0788000001",
                "alice@example.org", "50000", MembershipStatus.ACTIVE, RiskStatus.NORMAL);
        Member patrick = member("STF-002", "Patrick Niyonzima", "Operations", "0788000002",
                "patrick@example.org", "40000", MembershipStatus.ACTIVE, RiskStatus.NORMAL);
        Member grace = member("STF-003", "Grace Uwase", "Customer Service", "0788000003",
                "grace@example.org", "50000", MembershipStatus.ACTIVE, RiskStatus.NORMAL);
        Member eric = member("STF-004", "Eric Habimana", "ICT", "0788000004",
                "eric@example.org", "60000", MembershipStatus.ACTIVE, RiskStatus.NORMAL);
        Member diane = member("STF-005", "Diane Uwera", "Administration", "0788000005",
                "diane@example.org", "35000", MembershipStatus.ACTIVE, RiskStatus.NORMAL);
        Member jean = member("STF-006", "Jean Mugisha", "Logistics", "0788000006",
                "jean@example.org", "30000", MembershipStatus.LEFT, RiskStatus.WATCHLIST);
        jean.exitDate = LocalDate.now().minusMonths(2);
        members.saveAll(List.of(alice, patrick, grace, eric, diane, jean));

        AppUser admin = user("admin", "System Administrator", "admin@example.org",
                Role.ADMIN, "Admin@123", null);
        AppUser initiator = user("initiator", "Savings Initiator", "initiator@example.org",
                Role.INITIATOR, "Initiator@123", null);
        AppUser approver = user("approver", "Finance Approver", "approver@example.org",
                Role.APPROVER, "Approver@123", null);
        AppUser memberUser = user("member", alice.fullName, alice.email,
                Role.MEMBER, "Member@123", alice);
        users.saveAll(List.of(admin, initiator, approver, memberUser));

        FinanceCategory interest = category("Interest income", CategoryType.INCOME);
        FinanceCategory membership = category("Membership fees", CategoryType.INCOME);
        FinanceCategory bank = category("Bank charges", CategoryType.EXPENSE);
        FinanceCategory office = category("Office supplies", CategoryType.EXPENSE);
        categories.saveAll(List.of(interest, membership, bank, office));

        setting("currency", "RWF", "Currency used in the application");
        setting("defaultLoanInterestRate", "12.00", "Default annual flat loan interest rate");
        setting("minimumSavingsBalance", "0.00", "Minimum balance allowed after a withdrawal");
        setting("allowLoansForLeftMembers", "false", "Whether members who left may receive a new loan");

        for (Member member : List.of(alice, patrick, grace, eric, diane, jean)) {
            SavingsTransaction saving = new SavingsTransaction();
            saving.member = member;
            saving.savingType = SavingType.MONTHLY;
            saving.amount = member.monthlySavingAmount.multiply(new BigDecimal("6"));
            saving.transactionDate = LocalDate.now().minusMonths(1).withDayOfMonth(25);
            saving.reference = References.next("SAV");
            saving.description = "Opening approved savings balance";
            saving.createdBy = initiator;
            saving.submittedBy = initiator;
            saving.submittedAt = java.time.Instant.now();
            saving.actionedBy = approver;
            saving.actionedAt = java.time.Instant.now();
            saving.workflowStatus = WorkflowStatus.APPROVED;
            savings.save(saving);
        }

        Loan activeLoan = new Loan();
        activeLoan.applicationNumber = References.next("LOAN");
        activeLoan.member = jean;
        activeLoan.applicationDate = LocalDate.now().minusMonths(4);
        activeLoan.requestedAmount = new BigDecimal("1200000.00");
        activeLoan.approvedAmount = new BigDecimal("1200000.00");
        activeLoan.annualInterestRate = new BigDecimal("12.00");
        activeLoan.repaymentMonths = 12;
        activeLoan.purpose = "Home improvement";
        activeLoan.totalInterest = new BigDecimal("144000.00");
        activeLoan.totalPayable = new BigDecimal("1344000.00");
        activeLoan.monthlyInstallment = new BigDecimal("112000.00");
        activeLoan.outstandingBalance = new BigDecimal("1008000.00");
        activeLoan.workflowStatus = WorkflowStatus.APPROVED;
        activeLoan.loanStatus = LoanStatus.ACTIVE;
        activeLoan.disbursementDate = LocalDate.now().minusMonths(3);
        activeLoan.disbursementReference = References.next("DISB");
        activeLoan.createdBy = initiator;
        activeLoan.submittedBy = initiator;
        activeLoan.submittedAt = java.time.Instant.now();
        activeLoan.actionedBy = approver;
        activeLoan.actionedAt = java.time.Instant.now();
        loans.save(activeLoan);

        for (int i = 1; i <= 12; i++) {
            LoanSchedule row = new LoanSchedule();
            row.loan = activeLoan;
            row.installmentNumber = i;
            row.dueDate = activeLoan.disbursementDate.plusMonths(i);
            row.expectedAmount = new BigDecimal("112000.00");
            row.amountPaid = i <= 3 ? new BigDecimal("112000.00") : BigDecimal.ZERO;
            row.paymentStatus = i <= 3 ? PaymentStatus.PAID : PaymentStatus.PENDING;
            schedules.save(row);
        }

        LoanRepayment payment = new LoanRepayment();
        payment.loan = activeLoan;
        payment.amount = new BigDecimal("336000.00");
        payment.paymentDate = LocalDate.now().minusDays(10);
        payment.reference = References.next("PAY");
        payment.remarks = "Opening repayment history";
        payment.createdBy = initiator;
        payment.submittedBy = initiator;
        payment.submittedAt = java.time.Instant.now();
        payment.actionedBy = approver;
        payment.actionedAt = java.time.Instant.now();
        payment.workflowStatus = WorkflowStatus.APPROVED;
        repayments.save(payment);

        FinanceTransaction income = finance(FinanceType.INCOME, membership,
                "Annual membership fees", "450000.00", initiator, approver);
        FinanceTransaction expense = finance(FinanceType.EXPENSE, bank,
                "Quarterly bank fees", "65000.00", initiator, approver);
        finances.saveAll(List.of(income, expense));
    }

    private Member member(String code, String name, String department, String phone, String email,
                          String saving, MembershipStatus status, RiskStatus risk) {
        Member member = new Member();
        member.memberCode = code;
        member.fullName = name;
        member.department = department;
        member.phone = phone;
        member.email = email;
        member.monthlySavingAmount = new BigDecimal(saving);
        member.membershipStatus = status;
        member.riskStatus = risk;
        member.joiningDate = LocalDate.now().minusYears(2);
        return member;
    }

    private AppUser user(String username, String name, String email, Role role,
                         String password, Member member) {
        AppUser user = new AppUser();
        user.username = username;
        user.fullName = name;
        user.email = email;
        user.role = role;
        user.passwordHash = passwords.encode(password);
        user.enabled = true;
        user.member = member;
        return user;
    }

    private FinanceCategory category(String name, CategoryType type) {
        FinanceCategory category = new FinanceCategory();
        category.name = name;
        category.categoryType = type;
        category.active = true;
        return category;
    }

    private void setting(String key, String value, String description) {
        SystemSetting setting = new SystemSetting();
        setting.settingKey = key;
        setting.settingValue = value;
        setting.description = description;
        settings.save(setting);
    }

    private FinanceTransaction finance(FinanceType type, FinanceCategory category, String description,
                                       String amount, AppUser initiator, AppUser approver) {
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.reference = References.next(type == FinanceType.INCOME ? "INC" : "EXP");
        transaction.transactionDate = LocalDate.now().minusDays(15);
        transaction.financeType = type;
        transaction.category = category;
        transaction.description = description;
        transaction.amount = new BigDecimal(amount);
        transaction.createdBy = initiator;
        transaction.submittedBy = initiator;
        transaction.submittedAt = java.time.Instant.now();
        transaction.actionedBy = approver;
        transaction.actionedAt = java.time.Instant.now();
        transaction.workflowStatus = WorkflowStatus.APPROVED;
        return transaction;
    }
}
