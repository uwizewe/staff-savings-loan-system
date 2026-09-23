package com.binava.stafffinance.repayment.repository;
import com.binava.stafffinance.repayment.entity.RepaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RepaymentAllocationRepository extends JpaRepository<RepaymentAllocation,Long> {
    List<RepaymentAllocation> findByRepaymentId(Long repaymentId);
}
