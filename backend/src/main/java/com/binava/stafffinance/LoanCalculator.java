package com.binava.stafffinance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Pure loan mathematics kept separate from persistence and HTTP code. */
final class LoanCalculator {
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    private LoanCalculator() {}

    static LoanTerms flatRate(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("Principal must be greater than zero");
        }
        if (annualRate == null || annualRate.signum() < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (months < 1) throw new IllegalArgumentException("Repayment period must be at least one month");

        BigDecimal cleanPrincipal = principal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal interest = cleanPrincipal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(months))
                .divide(TWELVE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = cleanPrincipal.add(interest).setScale(2, RoundingMode.HALF_UP);
        BigDecimal installment = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        return new LoanTerms(cleanPrincipal, interest, total, installment, schedule(total, months));
    }

    static List<BigDecimal> schedule(BigDecimal totalPayable, int months) {
        BigDecimal installment = totalPayable.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        List<BigDecimal> rows = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int number = 1; number <= months; number++) {
            BigDecimal amount = number == months
                    ? totalPayable.subtract(allocated).setScale(2, RoundingMode.HALF_UP)
                    : installment;
            rows.add(amount);
            allocated = allocated.add(amount);
        }
        return List.copyOf(rows);
    }

    record LoanTerms(BigDecimal principal, BigDecimal interest, BigDecimal totalPayable,
                     BigDecimal monthlyInstallment, List<BigDecimal> schedule) {}
}

