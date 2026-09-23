package com.binava.stafffinance.loan.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record ScheduleVersionView(Long id,int versionNumber,String scheduleType,String status,boolean active,
 LocalDate effectiveDate,LocalDate firstInstallmentDate,BigDecimal previousPrincipal,BigDecimal additionalAmount,
 BigDecimal principal,int previousTerm,int term,BigDecimal annualRate,String createdBy,Instant createdAt,
 String submittedBy,Instant submittedAt,String actionedBy,Instant actionedAt,String remarks,String decisionRemarks,List<ScheduleView> rows) {}
