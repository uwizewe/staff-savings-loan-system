package com.binava.stafffinance.loan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Pure loan mathematics kept separate from persistence and HTTP code. */
public final class LoanCalculator {
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    private LoanCalculator() {}

    public record Installment(int number, BigDecimal openingBalance, BigDecimal principal,
            BigDecimal interest, BigDecimal amount, BigDecimal closingBalance) {}
    public record Breakdown(BigDecimal principal, BigDecimal annualRate, int months,
            BigDecimal interest, BigDecimal totalPayable, BigDecimal monthlyInstallment,
            List<Installment> rows) {}

    public static Breakdown reducingBalance(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal == null || principal.compareTo(BigDecimal.ONE) < 0 || annualRate == null
                || annualRate.signum() < 0 || annualRate.compareTo(new BigDecimal("100")) > 0
                || months < 1 || months > 120) throw new IllegalArgumentException("Invalid loan terms");
        var mc = java.math.MathContext.DECIMAL128;
        BigDecimal balance = principal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = annualRate.divide(TWELVE_HUNDRED, mc);
        BigDecimal factor = BigDecimal.ONE.add(rate).pow(months, mc);
        BigDecimal payment = (rate.signum() == 0 ? balance.divide(BigDecimal.valueOf(months), mc)
                : balance.multiply(rate, mc).multiply(factor, mc).divide(factor.subtract(BigDecimal.ONE), mc))
                .setScale(2, RoundingMode.HALF_UP);
        List<Installment> rows = new ArrayList<>();
        BigDecimal interestTotal = BigDecimal.ZERO;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = balance.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital = i == months ? balance : payment.subtract(interest).max(BigDecimal.ZERO).min(balance);
            BigDecimal closing = balance.subtract(capital);
            rows.add(new Installment(i, balance, capital, interest, capital.add(interest), closing));
            interestTotal = interestTotal.add(interest);
            balance = closing;
        }
        return new Breakdown(principal.setScale(2, RoundingMode.HALF_UP), annualRate, months,
                interestTotal, principal.setScale(2, RoundingMode.HALF_UP).add(interestTotal), payment, List.copyOf(rows));
    }

    public static LoanTerms flatRate(BigDecimal principal, BigDecimal annualRate, int months) {
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

    public static List<BigDecimal> schedule(BigDecimal totalPayable, int months) {
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

    public record LoanTerms(BigDecimal principal, BigDecimal interest, BigDecimal totalPayable,
                     BigDecimal monthlyInstallment, List<BigDecimal> schedule) {}
}
