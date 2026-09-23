package com.binava.stafffinance.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class Money {
    static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    public static BigDecimal amount(BigDecimal value) {
        if (value == null) return ZERO;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(List<BigDecimal> values) {
        return amount(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
