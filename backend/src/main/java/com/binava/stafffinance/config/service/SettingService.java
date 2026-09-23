package com.binava.stafffinance.config.service;

import com.binava.stafffinance.config.repository.SettingRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {
    private final SettingRepository settings;

    public SettingService(SettingRepository settings) { this.settings = settings; }

    @Transactional(readOnly = true)
    public BigDecimal decimal(String key, BigDecimal fallback) {
        return settings.findBySettingKey(key)
                .map(setting -> new BigDecimal(setting.settingValue))
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public boolean bool(String key, boolean fallback) {
        return settings.findBySettingKey(key)
                .map(setting -> Boolean.parseBoolean(setting.settingValue))
                .orElse(fallback);
    }
}
