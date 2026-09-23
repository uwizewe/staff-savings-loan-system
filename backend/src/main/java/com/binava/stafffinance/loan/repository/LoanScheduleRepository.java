package com.binava.stafffinance.loan.repository;

import com.binava.stafffinance.loan.entity.LoanSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {
    public List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);
    public void deleteByLoanId(Long loanId);
}
