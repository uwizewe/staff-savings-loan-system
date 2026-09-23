package com.binava.stafffinance.config.entity;

import com.binava.stafffinance.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    public String settingKey;

    @Column(nullable = false, length = 500)
    public String settingValue;

    @Column(nullable = false, length = 250)
    public String description;
}
