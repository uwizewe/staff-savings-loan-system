package com.binava.stafffinance.loan.repository;
import com.binava.stafffinance.loan.entity.LoanScheduleVersion;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface LoanScheduleVersionRepository extends JpaRepository<LoanScheduleVersion,Long> {
 @EntityGraph(attributePaths={"createdBy","submittedBy","actionedBy"}) List<LoanScheduleVersion> findByLoanIdOrderByVersionNumberDesc(Long loanId);
 @Override @EntityGraph(attributePaths={"createdBy","submittedBy","actionedBy"}) Optional<LoanScheduleVersion> findById(Long id);
}
