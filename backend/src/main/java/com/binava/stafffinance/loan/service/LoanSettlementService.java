package com.binava.stafffinance.loan.service;
import com.binava.stafffinance.loan.entity.Loan;
import com.binava.stafffinance.loan.entity.LoanStatus;
import com.binava.stafffinance.config.service.SettingService;
import com.binava.stafffinance.common.Money;
import com.binava.stafffinance.exception.BusinessException;
import java.math.*;
import org.springframework.stereotype.Service;
@Service
public class LoanSettlementService {
    public record Quote(BigDecimal remainingPrincipal,BigDecimal applicableInterest,BigDecimal charges,
                        BigDecimal settlementAmount,BigDecimal interestWaived,String policy) {}
    private final InstallmentScheduleService schedules;
    private final SettingService settings;
    public LoanSettlementService(InstallmentScheduleService schedules,SettingService settings) { this.schedules=schedules; this.settings=settings; }
    public Quote quote(Loan loan) {
        if(loan.loanStatus!=LoanStatus.ACTIVE) throw new BusinessException("Only active loans can be settled");
        if(loan.adjustmentType!=null) throw new BusinessException("Resolve the pending loan change first");
        var rows=schedules.active(loan);
        if(rows.stream().anyMatch(r->r.principalAmount==null)) throw new BusinessException("Legacy principal/interest allocation must be completed first");
        BigDecimal principal=schedules.remainingPrincipal(loan);
        BigDecimal scheduledInterest=rows.stream().map(r->r.interestAmount.subtract(r.amountPaid.min(r.interestAmount)).subtract(Money.amount(r.interestWaived)).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO,BigDecimal::add);
        // User policy: one monthly term of interest on the remaining principal, capped by unpaid contractual interest.
        BigDecimal interest=principal.multiply(loan.annualInterestRate).divide(new BigDecimal("1200"),2,RoundingMode.HALF_UP).min(scheduledInterest);
        BigDecimal charges=Money.amount(settings.decimal("loanSettlementCharge",BigDecimal.ZERO));
        if(charges.signum()<0) throw new BusinessException("Settlement charges cannot be negative");
        return new Quote(principal,interest,charges,principal.add(interest).add(charges),scheduledInterest.subtract(interest),"Remaining principal plus one monthly term of interest; future interest is waived.");
    }
}
