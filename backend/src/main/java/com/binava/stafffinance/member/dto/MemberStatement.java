package com.binava.stafffinance.member.dto;

import com.binava.stafffinance.loan.dto.LoanView;
import com.binava.stafffinance.repayment.dto.RepaymentView;
import com.binava.stafffinance.savings.dto.SavingView;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record MemberStatement(MemberView member, List<SavingView> savings, List<LoanView> loans,
                       List<RepaymentView> repayments, BigDecimal totalSavings,
                       BigDecimal totalRepaid, BigDecimal outstandingLoans) {}
