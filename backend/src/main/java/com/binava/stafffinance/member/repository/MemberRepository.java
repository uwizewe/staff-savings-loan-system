package com.binava.stafffinance.member.repository;

import com.binava.stafffinance.member.entity.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    public Optional<Member> findByMemberCodeIgnoreCase(String memberCode);
    public boolean existsByMemberCodeIgnoreCase(String memberCode);
    public List<Member> findAllByOrderByFullNameAsc();
}
