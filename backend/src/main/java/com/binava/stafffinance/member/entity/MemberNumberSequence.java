package com.binava.stafffinance.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_number_sequences")
public class MemberNumberSequence {
    @Id
    @Column(name = "sequence_name", length = 32)
    public String name;

    @Column(name = "last_number", nullable = false)
    public long lastNumber;
}
