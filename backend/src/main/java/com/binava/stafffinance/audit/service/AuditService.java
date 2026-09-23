package com.binava.stafffinance.audit.service;

import com.binava.stafffinance.audit.entity.AuditLog;
import com.binava.stafffinance.audit.repository.AuditLogRepository;
import com.binava.stafffinance.user.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) { this.logs = logs; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(AppUser user, String action, String entityType, Long entityId, String reference,
             Object previousStatus, Object newStatus, String details) {
        AuditLog log = new AuditLog();
        log.user = user;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.reference = reference;
        log.previousStatus = previousStatus == null ? null : previousStatus.toString();
        log.newStatus = newStatus == null ? null : newStatus.toString();
        log.details = details;
        logs.save(log);
    }
}
