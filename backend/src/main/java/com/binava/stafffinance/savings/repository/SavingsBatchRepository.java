package com.binava.stafffinance.savings.repository;

import com.binava.stafffinance.savings.entity.SavingsBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsBatchRepository extends JpaRepository<SavingsBatch, Long> {
    @Override
    @EntityGraph(attributePaths = {"createdBy", "actionedBy"})
    public Optional<SavingsBatch> findById(Long id);

    @EntityGraph(attributePaths = {"createdBy", "actionedBy"})
    public List<SavingsBatch> findAllByOrderByCreatedAtDesc();
}
