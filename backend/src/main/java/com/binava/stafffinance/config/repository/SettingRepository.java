package com.binava.stafffinance.config.repository;

import com.binava.stafffinance.config.entity.SystemSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SystemSetting, Long> {
    public Optional<SystemSetting> findBySettingKey(String key);
    public List<SystemSetting> findAllByOrderBySettingKeyAsc();
}
