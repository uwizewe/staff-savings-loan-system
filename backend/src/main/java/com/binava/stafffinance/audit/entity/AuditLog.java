package com.binava.stafffinance.audit.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import com.binava.stafffinance.user.entity.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_time", columnList = "created_at"))
public class AuditLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    public AppUser user;

    @Column(nullable = false, length = 80)
    public String action;

    @Column(nullable = false, length = 80)
    public String entityType;

    public Long entityId;

    @Column(length = 100)
    public String reference;

    @Column(length = 30)
    public String previousStatus;

    @Column(length = 30)
    public String newStatus;

    @Column(length = 1000)
    public String details;
}
