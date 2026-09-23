package com.binava.stafffinance.repayment.repository;

import com.binava.stafffinance.repayment.entity.RepaymentBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentBatchRepository extends JpaRepository<RepaymentBatch, Long> {
    @Override
    @EntityGraph(attributePaths = {"createdBy", "actionedBy"})
    public Optional<RepaymentBatch> findById(Long id);

    @EntityGraph(attributePaths = {"createdBy", "actionedBy"})
    public List<RepaymentBatch> findAllByOrderByCreatedAtDesc();
}
