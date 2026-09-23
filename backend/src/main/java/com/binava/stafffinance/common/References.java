package com.binava.stafffinance.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class References {
    private References() {}

    public static String next(String prefix) {
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
