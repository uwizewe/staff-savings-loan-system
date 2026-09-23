package com.binava.stafffinance.user.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.role.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_user_username", columnList = "username", unique = true))
public class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false, length = 160)
    public String fullName;

    @Column(nullable = false, length = 160)
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Role role;

    @Column(nullable = false)
    public boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    public Member member;
}
