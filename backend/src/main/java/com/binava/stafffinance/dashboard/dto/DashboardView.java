package com.binava.stafffinance.dashboard.dto;

import com.binava.stafffinance.approval.dto.ApprovalView;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardView(long totalMembers, long activeMembers, BigDecimal totalSavings,
                     long activeLoans, BigDecimal totalOutstandingLoans,
                     BigDecimal totalLoanRepayments, BigDecimal totalIncome,
                     BigDecimal totalExpenses, long membersWithOutstandingLoans,
                     long leftMembersWithOutstandingLoans, long pendingApprovals,
                     List<Map<String, Object>> monthlyTrend,
                     List<ApprovalView> recentApprovals) {}
