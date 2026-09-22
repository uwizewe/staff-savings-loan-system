package com.binava.stafffinance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LoanCalculatorTest {
    @Test
    void calculatesFlatInterestAndExactScheduleTotal() {
        var terms = LoanCalculator.flatRate(new BigDecimal("1200000"), new BigDecimal("12"), 12);

        assertEquals(0, terms.interest().compareTo(new BigDecimal("144000.00")));
        assertEquals(0, terms.totalPayable().compareTo(new BigDecimal("1344000.00")));
        assertEquals(0, terms.monthlyInstallment().compareTo(new BigDecimal("112000.00")));
        assertEquals(0, terms.totalPayable().compareTo(
                terms.schedule().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void putsRoundingDifferenceInLastInstallment() {
        var terms = LoanCalculator.flatRate(new BigDecimal("100000"), new BigDecimal("10"), 3);

        assertEquals(3, terms.schedule().size());
        assertEquals(0, terms.totalPayable().compareTo(
                terms.schedule().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void rejectsInvalidTerms() {
        assertThrows(IllegalArgumentException.class,
                () -> LoanCalculator.flatRate(BigDecimal.ZERO, BigDecimal.TEN, 12));
        assertThrows(IllegalArgumentException.class,
                () -> LoanCalculator.flatRate(BigDecimal.TEN, new BigDecimal("-1"), 12));
        assertThrows(IllegalArgumentException.class,
                () -> LoanCalculator.flatRate(BigDecimal.TEN, BigDecimal.ONE, 0));
    }
}

