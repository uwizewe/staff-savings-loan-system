package com.binava.stafffinance.loan.entity;
import com.binava.stafffinance.approval.entity.WorkflowEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="loan_schedule_versions",uniqueConstraints=@UniqueConstraint(columnNames={"loan_id","version_number"}))
public class LoanScheduleVersion extends WorkflowEntity {
 @Column(nullable=false) public Long loanId;
 public int versionNumber;
 public String scheduleType;
 public LocalDate effectiveDate;
 public LocalDate firstInstallmentDate;
 @Column(precision=19,scale=2) public BigDecimal previousPrincipal;
 @Column(precision=19,scale=2) public BigDecimal additionalAmount;
 @Column(precision=19,scale=2) public BigDecimal principal;
 @Column(precision=19,scale=2) public BigDecimal interest;
 @Column(precision=19,scale=2) public BigDecimal totalPayable;
 @Column(precision=19,scale=2) public BigDecimal installment;
 @Column(precision=7,scale=3) public BigDecimal annualRate;
 public int previousTerm;
 public int term;
 @Column(length=1000) public String remarks;
}
