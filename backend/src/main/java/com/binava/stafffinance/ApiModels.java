package com.binava.stafffinance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

record LoginRequest(@NotBlank String username, @NotBlank String password) {}
record LoginResponse(String token, UserView user) {}

record UserView(Long id, String username, String fullName, String email, Role role,
                boolean enabled, Long memberId) {}

record UserRequest(@NotBlank String username, @NotBlank String fullName,
                   @Email @NotBlank String email, @NotNull Role role,
                   String password, Boolean enabled, Long memberId) {}

record PasswordRequest(@NotBlank @Size(min = 8) String password) {}

record MemberRequest(
        @NotBlank String memberCode,
        @NotBlank String fullName,
        @NotBlank String department,
        @NotBlank String phone,
        @Email @NotBlank String email,
        @NotNull @DecimalMin("0.00") BigDecimal monthlySavingAmount,
        @NotNull MembershipStatus membershipStatus,
        @NotNull RiskStatus riskStatus,
        @NotNull LocalDate joiningDate,
        LocalDate exitDate,
        String remarks) {}

record MemberView(Long id, String memberCode, String fullName, String department,
                  String phone, String email, BigDecimal monthlySavingAmount,
                  MembershipStatus membershipStatus, RiskStatus riskStatus,
                  LocalDate joiningDate, LocalDate exitDate, String remarks,
                  BigDecimal totalSavings, BigDecimal outstandingLoans) {}

record SavingRequest(@NotNull Long memberId,
                     @NotNull @DecimalMin("0.01") BigDecimal amount,
                     @NotNull LocalDate transactionDate,
                     String reference,
                     String description) {}

record SavingsBatchItemRequest(@NotNull Long memberId,
                               @NotNull @DecimalMin("0.01") BigDecimal amount) {}

record SavingsBatchRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
                           @NotEmpty List<@Valid SavingsBatchItemRequest> items,
                           String remarks) {}

record SavingView(Long id, Long memberId, String memberName, SavingType savingType,
                  BigDecimal amount, LocalDate transactionDate, String reference,
                  String description, WorkflowStatus status, String createdBy,
                  String actionedBy, Instant createdAt) {}

record BatchView(Long id, String period, BigDecimal totalAmount, WorkflowStatus status,
                 String createdBy, String actionedBy, Instant createdAt, String remarks) {}

record LoanRequest(@NotNull Long memberId,
                   @NotNull LocalDate applicationDate,
                   @NotNull @DecimalMin("1.00") BigDecimal requestedAmount,
                   @NotNull @DecimalMin("0.00") BigDecimal annualInterestRate,
                   @Min(1) @Max(120) int repaymentMonths,
                   @NotBlank String purpose,
                   String remarks) {}

record LoanDecisionRequest(String remarks, @DecimalMin("1.00") BigDecimal approvedAmount,
                           @DecimalMin("0.00") BigDecimal annualInterestRate,
                           @Min(1) @Max(120) Integer repaymentMonths) {}

record DisbursementRequest(@NotNull LocalDate disbursementDate, @NotBlank String reference) {}

record LoanView(Long id, String applicationNumber, Long memberId, String memberName,
                LocalDate applicationDate, BigDecimal requestedAmount, BigDecimal approvedAmount,
                BigDecimal annualInterestRate, int repaymentMonths, String purpose,
                BigDecimal monthlyInstallment, BigDecimal totalInterest, BigDecimal totalPayable,
                BigDecimal amountRepaid, BigDecimal outstandingBalance, WorkflowStatus workflowStatus,
                LoanStatus loanStatus, LocalDate disbursementDate, String disbursementReference,
                String createdBy, String actionedBy, String remarks, Instant createdAt) {}

record ScheduleView(Long id, int installmentNumber, LocalDate dueDate,
                    BigDecimal expectedAmount, BigDecimal amountPaid,
                    BigDecimal remainingAmount, PaymentStatus paymentStatus) {}

record RepaymentRequest(@NotNull Long loanId,
                        @NotNull @DecimalMin("0.01") BigDecimal amount,
                        @NotNull LocalDate paymentDate,
                        String reference,
                        String remarks) {}

record RepaymentBatchItemRequest(@NotNull Long loanId,
                                 @NotNull @DecimalMin("0.01") BigDecimal amount) {}

record RepaymentBatchRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
                             @NotEmpty List<@Valid RepaymentBatchItemRequest> items,
                             String remarks) {}

record RepaymentView(Long id, Long loanId, String loanNumber, Long memberId, String memberName,
                     BigDecimal amount, LocalDate paymentDate, String reference,
                     WorkflowStatus status, String createdBy, String actionedBy,
                     String remarks, Instant createdAt, Long batchId) {}

record FinanceRequest(@NotNull FinanceType financeType,
                      @NotNull Long categoryId,
                      @NotNull LocalDate transactionDate,
                      @NotBlank String description,
                      @NotNull @DecimalMin("0.01") BigDecimal amount,
                      String reference,
                      String supportingReference) {}

record FinanceView(Long id, String reference, LocalDate transactionDate,
                   FinanceType financeType, Long categoryId, String category,
                   String description, BigDecimal amount, String supportingReference,
                   WorkflowStatus status, String createdBy, String actionedBy,
                   Instant createdAt) {}

record CategoryRequest(@NotBlank String name, @NotNull CategoryType categoryType,
                       Boolean active) {}
record CategoryView(Long id, String name, CategoryType categoryType, boolean active) {}

record DecisionRequest(boolean approve, String remarks) {}

record ApprovalView(String type, Long id, String reference, String description,
                    BigDecimal amount, String createdBy, Instant submittedAt) {}

record DashboardView(long totalMembers, long activeMembers, BigDecimal totalSavings,
                     long activeLoans, BigDecimal totalOutstandingLoans,
                     BigDecimal totalLoanRepayments, BigDecimal totalIncome,
                     BigDecimal totalExpenses, long membersWithOutstandingLoans,
                     long leftMembersWithOutstandingLoans, long pendingApprovals,
                     List<Map<String, Object>> monthlyTrend,
                     List<ApprovalView> recentApprovals) {}

record AuditView(Long id, String user, String action, String entityType, Long entityId,
                 String reference, String previousStatus, String newStatus,
                 String details, Instant createdAt) {}

record SettingView(Long id, String key, String value, String description) {}
record SettingRequest(@NotBlank String value) {}

record MemberStatement(MemberView member, List<SavingView> savings, List<LoanView> loans,
                       List<RepaymentView> repayments, BigDecimal totalSavings,
                       BigDecimal totalRepaid, BigDecimal outstandingLoans) {}
