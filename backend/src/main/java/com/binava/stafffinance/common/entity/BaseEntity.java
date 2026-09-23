package com.binava.stafffinance.common.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    public Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    public Instant updatedAt;

    @Version
    public Long version;
}
