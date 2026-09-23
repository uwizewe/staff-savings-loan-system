package com.binava.stafffinance.loan.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReducingBalanceTest {
    @Test void reducingScheduleReconcilesPrincipalInterestAndFinalBalance() {
        var terms = LoanCalculator.reducingBalance(new BigDecimal("1000"), new BigDecimal("12"), 12);
        assertEquals(new BigDecimal("88.85"), terms.monthlyInstallment());
        assertEquals(new BigDecimal("10.00"), terms.rows().get(0).interest());
        assertEquals(new BigDecimal("78.85"), terms.rows().get(0).principal());
        assertEquals(new BigDecimal("0.00"), terms.rows().get(11).closingBalance());
        assertEquals(0, terms.rows().stream().map(LoanCalculator.Installment::principal).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(terms.principal()));
        assertEquals(0, terms.rows().stream().map(LoanCalculator.Installment::amount).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(terms.totalPayable()));
        for (int i=1; i<12; i++) assertTrue(terms.rows().get(i).interest().compareTo(terms.rows().get(i-1).interest()) <= 0);
    }
    @Test void zeroRateAndRoundingRemainExact() {
        var terms = LoanCalculator.reducingBalance(new BigDecimal("100"), BigDecimal.ZERO, 3);
        assertEquals(new BigDecimal("33.34"), terms.rows().get(2).amount());
        assertEquals(new BigDecimal("0.00"), terms.interest());
        var tiny = LoanCalculator.reducingBalance(BigDecimal.ONE, BigDecimal.ZERO, 120);
        assertEquals(new BigDecimal("0.00"), tiny.rows().get(119).closingBalance());
        assertTrue(tiny.rows().stream().allMatch(r -> r.principal().signum() >= 0));
    }
}
