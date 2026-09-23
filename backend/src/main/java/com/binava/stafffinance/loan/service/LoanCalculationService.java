package com.binava.stafffinance.loan.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class LoanCalculationService {
 public record Row(int number, LocalDate dueDate, BigDecimal openingBalance, BigDecimal principal,
    BigDecimal interest, BigDecimal amount, BigDecimal closingBalance, String paymentStatus) {}
 public record Calculation(BigDecimal principal, BigDecimal annualRate, int months, BigDecimal interest,
    BigDecimal totalPayable, BigDecimal monthlyInstallment, List<Row> rows) {}
 public Calculation calculate(BigDecimal principal, BigDecimal rate, int term, LocalDate firstDate) {
  if(firstDate==null) throw new IllegalArgumentException("First installment date is required");
  var terms=LoanCalculator.reducingBalance(principal,rate,term);
  return new Calculation(terms.principal(),rate,term,terms.interest(),terms.totalPayable(),terms.monthlyInstallment(),
    terms.rows().stream().map(r->new Row(r.number(),firstDate.plusMonths(r.number()-1),r.openingBalance(),r.principal(),r.interest(),r.amount(),r.closingBalance(),"PENDING")).toList());
 }
}
