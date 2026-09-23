package com.binava.stafffinance.approval.service;

import com.binava.stafffinance.approval.entity.WorkflowEntity;
import com.binava.stafffinance.approval.entity.WorkflowStatus;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.user.entity.AppUser;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class WorkflowService {
    private final AuthService auth;
    private final AuditService audit;

    public WorkflowService(AuthService auth, AuditService audit) {
        this.auth = auth;
        this.audit = audit;
    }

    public AppUser submit(WorkflowEntity entity, String type, String reference) {
        if (entity.workflowStatus != WorkflowStatus.DRAFT && entity.workflowStatus != WorkflowStatus.REJECTED) {
            throw new BusinessException("Only draft or rejected records can be submitted");
        }
        AppUser user = auth.currentUser();
        WorkflowStatus previous = entity.workflowStatus;
        entity.workflowStatus = WorkflowStatus.PENDING_APPROVAL;
        entity.submittedBy = user;
        entity.submittedAt = Instant.now();
        entity.actionedBy = null;
        entity.actionedAt = null;
        entity.decisionRemarks = null;
        audit.log(user, "SUBMIT", type, entity.id, reference, previous,
                entity.workflowStatus, "Submitted for approval");
        return user;
    }

    public AppUser decide(WorkflowEntity entity, boolean approve, String remarks, String type, String reference) {
        if (entity.workflowStatus != WorkflowStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending records can be approved or rejected");
        }
        AppUser user = auth.currentUser();
        if (entity.createdBy.id.equals(user.id)) {
            throw new BusinessException("The person who created a transaction cannot approve it");
        }
        WorkflowStatus previous = entity.workflowStatus;
        entity.workflowStatus = approve ? WorkflowStatus.APPROVED : WorkflowStatus.REJECTED;
        entity.actionedBy = user;
        entity.actionedAt = Instant.now();
        entity.decisionRemarks = remarks;
        audit.log(user, approve ? "APPROVE" : "REJECT", type, entity.id, reference,
                previous, entity.workflowStatus, remarks);
        return user;
    }
}
