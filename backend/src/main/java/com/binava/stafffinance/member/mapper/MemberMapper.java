package com.binava.stafffinance.member.mapper;

import com.binava.stafffinance.member.dto.MemberView;
import com.binava.stafffinance.member.entity.Member;
import java.math.BigDecimal;

public final class MemberMapper {
    private MemberMapper() {}

    public static MemberView member(Member member, BigDecimal savingsBalance, BigDecimal outstandingBalance) {
        return new MemberView(member.id, member.memberCode, member.fullName, member.department,
                member.phone, member.email, member.monthlySavingAmount, member.membershipStatus,
                member.riskStatus, member.joiningDate, member.exitDate, member.remarks,
                savingsBalance, outstandingBalance);
    }
}