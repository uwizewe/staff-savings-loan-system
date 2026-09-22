package com.binava.stafffinance;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class BusinessException extends RuntimeException {
    BusinessException(String message) { super(message); }
}

class NotFoundException extends RuntimeException {
    NotFoundException(String message) { super(message); }
}

class UnauthorizedException extends RuntimeException {
    UnauthorizedException(String message) { super(message); }
}

record ApiError(Instant timestamp, int status, String error, String message,
                String path, Map<String, String> fieldErrors) {}

@RestControllerAdvice
class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, null);
    }

    @ExceptionHandler({BusinessException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> business(Exception exception, HttpServletRequest request) {
        String message = exception instanceof DataIntegrityViolationException
                ? "This record conflicts with existing data. Check unique references and codes."
                : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "You do not have permission for this action", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "Please correct the highlighted information", request, errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Check the server log for details.", request, null);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message,
                                              HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fields));
    }
}

final class References {
    private References() {}

    static String next(String prefix) {
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

final class Money {
    static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    static BigDecimal amount(BigDecimal value) {
        if (value == null) return ZERO;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal sum(List<BigDecimal> values) {
        return amount(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}

final class Views {
    private Views() {}

    static UserView user(AppUser user) {
        return new UserView(user.id, user.username, user.fullName, user.email, user.role,
                user.enabled, user.member == null ? null : user.member.id);
    }

    static SavingView saving(SavingsTransaction saving) {
        return new SavingView(saving.id, saving.member.id, saving.member.fullName, saving.savingType,
                saving.amount, saving.transactionDate, saving.reference, saving.description,
                saving.workflowStatus, saving.createdBy.fullName,
                saving.actionedBy == null ? null : saving.actionedBy.fullName, saving.createdAt);
    }

    static BatchView savingsBatch(SavingsBatch batch) {
        return new BatchView(batch.id, batch.period, batch.totalAmount, batch.workflowStatus,
                batch.createdBy.fullName, batch.actionedBy == null ? null : batch.actionedBy.fullName,
                batch.createdAt, batch.remarks);
    }

    static LoanView loan(Loan loan, BigDecimal amountRepaid) {
        return new LoanView(loan.id, loan.applicationNumber, loan.member.id, loan.member.fullName,
                loan.applicationDate, loan.requestedAmount, loan.approvedAmount,
                loan.annualInterestRate, loan.repaymentMonths, loan.purpose,
                loan.monthlyInstallment, loan.totalInterest, loan.totalPayable,
                Money.amount(amountRepaid), Money.amount(loan.outstandingBalance), loan.workflowStatus,
                loan.loanStatus, loan.disbursementDate, loan.disbursementReference,
                loan.createdBy.fullName, loan.actionedBy == null ? null : loan.actionedBy.fullName,
                loan.remarks, loan.createdAt);
    }

    static ScheduleView schedule(LoanSchedule schedule) {
        return new ScheduleView(schedule.id, schedule.installmentNumber, schedule.dueDate,
                schedule.expectedAmount, schedule.amountPaid,
                Money.amount(schedule.expectedAmount.subtract(schedule.amountPaid).max(BigDecimal.ZERO)),
                schedule.paymentStatus);
    }

    static RepaymentView repayment(LoanRepayment repayment) {
        return new RepaymentView(repayment.id, repayment.loan.id, repayment.loan.applicationNumber,
                repayment.loan.member.id, repayment.loan.member.fullName, repayment.amount,
                repayment.paymentDate, repayment.reference, repayment.workflowStatus,
                repayment.createdBy.fullName,
                repayment.actionedBy == null ? null : repayment.actionedBy.fullName,
                repayment.remarks, repayment.createdAt,
                repayment.batch == null ? null : repayment.batch.id);
    }

    static FinanceView finance(FinanceTransaction transaction) {
        return new FinanceView(transaction.id, transaction.reference, transaction.transactionDate,
                transaction.financeType, transaction.category.id, transaction.category.name,
                transaction.description, transaction.amount, transaction.supportingReference,
                transaction.workflowStatus, transaction.createdBy.fullName,
                transaction.actionedBy == null ? null : transaction.actionedBy.fullName,
                transaction.createdAt);
    }
}

@Service
class AuditService {
    private final AuditLogRepository logs;

    AuditService(AuditLogRepository logs) { this.logs = logs; }

    @Transactional(propagation = Propagation.MANDATORY)
    void log(AppUser user, String action, String entityType, Long entityId, String reference,
             Object previousStatus, Object newStatus, String details) {
        AuditLog log = new AuditLog();
        log.user = user;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.reference = reference;
        log.previousStatus = previousStatus == null ? null : previousStatus.toString();
        log.newStatus = newStatus == null ? null : newStatus.toString();
        log.details = details;
        logs.save(log);
    }
}

@Service
class WorkflowService {
    private final AuthService auth;
    private final AuditService audit;

    WorkflowService(AuthService auth, AuditService audit) {
        this.auth = auth;
        this.audit = audit;
    }

    AppUser submit(WorkflowEntity entity, String type, String reference) {
        if (entity.workflowStatus != WorkflowStatus.DRAFT && entity.workflowStatus != WorkflowStatus.REJECTED) {
            throw new BusinessException("Only draft or rejected records can be submitted");
        }
        AppUser user = auth.currentUser();
        WorkflowStatus previous = entity.workflowStatus;
        entity.workflowStatus = WorkflowStatus.PENDING_APPROVAL;
        entity.submittedBy = user;
        entity.submittedAt = Instant.now();
        entity.actionedBy = null;
        entity.actionedAt = null;
        entity.decisionRemarks = null;
        audit.log(user, "SUBMIT", type, entity.id, reference, previous,
                entity.workflowStatus, "Submitted for approval");
        return user;
    }

    AppUser decide(WorkflowEntity entity, boolean approve, String remarks, String type, String reference) {
        if (entity.workflowStatus != WorkflowStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending records can be approved or rejected");
        }
        AppUser user = auth.currentUser();
        if (entity.createdBy.id.equals(user.id)) {
            throw new BusinessException("The person who created a transaction cannot approve it");
        }
        WorkflowStatus previous = entity.workflowStatus;
        entity.workflowStatus = approve ? WorkflowStatus.APPROVED : WorkflowStatus.REJECTED;
        entity.actionedBy = user;
        entity.actionedAt = Instant.now();
        entity.decisionRemarks = remarks;
        audit.log(user, approve ? "APPROVE" : "REJECT", type, entity.id, reference,
                previous, entity.workflowStatus, remarks);
        return user;
    }
}

@Service
class SettingService {
    private final SettingRepository settings;

    SettingService(SettingRepository settings) { this.settings = settings; }

    @Transactional(readOnly = true)
    BigDecimal decimal(String key, BigDecimal fallback) {
        return settings.findBySettingKey(key)
                .map(setting -> new BigDecimal(setting.settingValue))
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    boolean bool(String key, boolean fallback) {
        return settings.findBySettingKey(key)
                .map(setting -> Boolean.parseBoolean(setting.settingValue))
                .orElse(fallback);
    }
}
