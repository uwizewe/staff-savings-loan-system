package com.binava.stafffinance.user.repository;

import com.binava.stafffinance.user.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    @EntityGraph(attributePaths = {"member"})
    public Optional<AppUser> findByUsernameIgnoreCase(String username);
    public boolean existsByUsernameIgnoreCase(String username);
}
