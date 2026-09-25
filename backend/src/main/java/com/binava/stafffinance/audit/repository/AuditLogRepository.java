package com.binava.stafffinance.audit.repository;

import com.binava.stafffinance.audit.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    public List<AuditLog> findTop250ByOrderByCreatedAtDesc();
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    public List<AuditLog> findAllByOrderByCreatedAtDesc();
}
