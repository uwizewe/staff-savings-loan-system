package com.binava.stafffinance.approval.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import com.binava.stafffinance.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
public abstract class WorkflowEntity extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public WorkflowStatus workflowStatus = WorkflowStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    public AppUser submittedBy;

    public Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    public AppUser actionedBy;

    public Instant actionedAt;

    @Column(length = 500)
    public String decisionRemarks;
}
