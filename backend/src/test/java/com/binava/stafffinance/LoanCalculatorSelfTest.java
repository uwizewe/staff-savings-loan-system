package com.binava.stafffinance;

import java.math.BigDecimal;

/** Can run with plain javac/java, even before Maven dependencies are downloaded. */
public class LoanCalculatorSelfTest {
    public static void main(String[] args) {
        var terms = LoanCalculator.flatRate(new BigDecimal("1200000"), new BigDecimal("12"), 12);
        assertEquals("144000.00", terms.interest());
        assertEquals("1344000.00", terms.totalPayable());
        assertEquals("112000.00", terms.monthlyInstallment());
        assertEquals("1344000.00", terms.schedule().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        var rounded = LoanCalculator.flatRate(new BigDecimal("100000"), new BigDecimal("10"), 3);
        assertEquals(3, rounded.schedule().size());
        assertEquals(rounded.totalPayable().toPlainString(),
                rounded.schedule().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        expectFailure(() -> LoanCalculator.flatRate(BigDecimal.ZERO, BigDecimal.TEN, 12));
        expectFailure(() -> LoanCalculator.flatRate(BigDecimal.TEN, new BigDecimal("-1"), 12));
        expectFailure(() -> LoanCalculator.flatRate(BigDecimal.TEN, BigDecimal.ONE, 0));
        System.out.println("LoanCalculatorSelfTest: all checks passed");
    }

    private static void assertEquals(String expected, BigDecimal actual) {
        if (actual.compareTo(new BigDecimal(expected)) != 0) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (actual != expected) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected calculation to fail");
        } catch (IllegalArgumentException expected) {
            // Expected validation path.
        }
    }
}

