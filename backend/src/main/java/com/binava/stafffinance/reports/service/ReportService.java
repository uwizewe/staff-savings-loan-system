package com.binava.stafffinance.reports.service;

import com.binava.stafffinance.member.service.MemberService;
import com.binava.stafffinance.savings.service.SavingsService;
import com.binava.stafffinance.loan.service.LoanService;
import com.binava.stafffinance.loan.service.InstallmentScheduleService;
import com.binava.stafffinance.loan.repository.LoanRepository;
import com.binava.stafffinance.repayment.repository.RepaymentRepository;
import com.binava.stafffinance.repayment.mapper.RepaymentMapper;
import com.binava.stafffinance.repayment.repository.RepaymentAllocationRepository;
import com.binava.stafffinance.finance.service.FinanceService;
import com.binava.stafffinance.config.repository.SettingRepository;
import com.binava.stafffinance.audit.repository.AuditLogRepository;
import com.binava.stafffinance.audit.dto.AuditView;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only, transactionally consistent report inputs. Reports never post financial transactions. */
@Service
public class ReportService {
    private final MemberService members;
    private final SavingsService savings;
    private final LoanService loans;
    private final LoanRepository loanRepository;
    private final InstallmentScheduleService schedules;
    private final RepaymentRepository payments;
    private final RepaymentAllocationRepository allocations;
    private final FinanceService finance;
    private final SettingRepository settings;
    private final AuditLogRepository audits;

    public ReportService(MemberService members, SavingsService savings, LoanService loans, LoanRepository loanRepository,
            InstallmentScheduleService schedules, RepaymentRepository payments, RepaymentAllocationRepository allocations,
            FinanceService finance, SettingRepository settings, AuditLogRepository audits) {
        this.members=members; this.savings=savings; this.loans=loans; this.loanRepository=loanRepository;
        this.schedules=schedules; this.payments=payments; this.allocations=allocations; this.finance=finance;
        this.settings=settings; this.audits=audits;
    }

    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Map<String,Object> snapshot() {
        var result=new LinkedHashMap<String,Object>();
        result.put("generatedAt",Instant.now());
        result.put("organization",settings.findBySettingKey("organizationName").map(s->s.settingValue).orElse("VFR Association"));
        result.put("currency",settings.findBySettingKey("currency").map(s->s.settingValue).orElse("RWF"));
        result.put("members",members.list(null,null,null));
        result.put("savings",savings.list(null,null,null));
        result.put("loans",loans.list(null,null));
        result.put("finance",finance.list(null,null));
        var sourcePayments=payments.findAllByOrderByPaymentDateDescCreatedAtDesc();
        result.put("repayments",sourcePayments.stream().map(RepaymentMapper::repayment).toList());
        var paymentDetails=new LinkedHashMap<Long,Object>();
        for(var payment:sourcePayments) {
            var detail=new LinkedHashMap<String,Object>();
            detail.put("interestWaived",payment.interestWaived);
            detail.put("actionedAt",payment.actionedAt);
            detail.put("scheduleVersionId",payment.scheduleVersionId);
            paymentDetails.put(payment.id,detail);
        }
        result.put("repaymentDetails",paymentDetails);
        var versions=new LinkedHashMap<Long,Object>();
        for(var loan:loanRepository.findAllByOrderByApplicationDateDescCreatedAtDesc()) versions.put(loan.id,schedules.history(loan));
        result.put("scheduleVersions",versions);
        result.put("allocations",allocations.findAll().stream().map(a->{
            var item=new LinkedHashMap<String,Object>();
            item.put("repaymentId",a.repaymentId); item.put("scheduleId",a.scheduleId); item.put("scheduleVersionId",a.scheduleVersionId);
            item.put("principal",a.principal); item.put("interest",a.interest); item.put("interestWaived",a.interestWaived);
            return item;
        }).toList());
        return result;
    }

    @Transactional(readOnly=true)
    public Map<String,Object> audit() {
        return Map.of("generatedAt",Instant.now(),"organization",settings.findBySettingKey("organizationName").map(s->s.settingValue).orElse("VFR Association"),
            "currency","RWF","audits",audits.findAllByOrderByCreatedAtDesc().stream()
            .map(item->new AuditView(item.id,item.user==null?"System":item.user.fullName,item.action,item.entityType,item.entityId,item.reference,item.previousStatus,item.newStatus,item.details,item.createdAt)).toList());
    }
}
