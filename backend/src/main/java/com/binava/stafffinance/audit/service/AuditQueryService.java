package com.binava.stafffinance.audit.service;

import com.binava.stafffinance.audit.dto.AuditView;
import com.binava.stafffinance.audit.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
    private final AuditLogRepository logs;

    public AuditQueryService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public List<AuditView> auditLogs() {
        return logs.findTop250ByOrderByCreatedAtDesc().stream()
                .map(item -> new AuditView(item.id, item.user == null ? "System" : item.user.fullName,
                        item.action, item.entityType, item.entityId, item.reference,
                        item.previousStatus, item.newStatus, item.details, item.createdAt))
                .toList();
    }
}
