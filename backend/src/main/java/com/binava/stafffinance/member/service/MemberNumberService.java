package com.binava.stafffinance.member.service;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Allocates registration numbers under a database lock shared by every application instance. */
@Service
public class MemberNumberService {
    private static final Pattern NUMBERED_CODE = Pattern.compile("^VFC-?([0-9]{1,18})$", Pattern.CASE_INSENSITIVE);
    private final JdbcTemplate jdbc;

    public MemberNumberService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextCode() {
        // The upsert also serializes first-use initialization on MySQL (and H2 MySQL test mode).
        jdbc.update("INSERT INTO member_number_sequences (sequence_name, last_number) VALUES ('VFC', 0) "
                + "ON DUPLICATE KEY UPDATE sequence_name = sequence_name");
        long highest = jdbc.queryForObject("SELECT last_number FROM member_number_sequences WHERE sequence_name = 'VFC' FOR UPDATE", Long.class);
        // Include existing/imported numeric registrations; UUID and other legacy identifiers are preserved.
        for (String code : jdbc.queryForList("SELECT member_code FROM members", String.class)) {
            var match = NUMBERED_CODE.matcher(code);
            if (match.matches()) highest = Math.max(highest, Long.parseLong(match.group(1)));
        }
        long next = Math.addExact(highest, 1);
        jdbc.update("UPDATE member_number_sequences SET last_number = ? WHERE sequence_name = 'VFC'", next);
        return String.format(Locale.ROOT, "VFC%03d", next);
    }
}
