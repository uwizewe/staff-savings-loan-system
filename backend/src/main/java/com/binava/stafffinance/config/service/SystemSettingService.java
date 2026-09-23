package com.binava.stafffinance.config.service;

import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.config.dto.SettingRequest;
import com.binava.stafffinance.config.dto.SettingView;
import com.binava.stafffinance.config.entity.SystemSetting;
import com.binava.stafffinance.config.repository.SettingRepository;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.user.entity.AppUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingService {
    private final SettingRepository settings;
    private final AuthService auth;
    private final AuditService audit;

    public SystemSettingService(SettingRepository settings, AuthService auth, AuditService audit) {
        this.settings = settings;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<SettingView> settings() {
        return settings.findAllByOrderBySettingKeyAsc().stream()
                .map(item -> new SettingView(item.id, item.settingKey, item.settingValue, item.description))
                .toList();
    }

    @Transactional
    public SettingView updateSetting(Long id, SettingRequest request) {
        SystemSetting setting = settings.findById(id)
                .orElseThrow(() -> new NotFoundException("Setting not found"));
        String previous = setting.settingValue;
        setting.settingValue = request.value().trim();
        AppUser actor = auth.currentUser();
        audit.log(actor, "UPDATE", "SETTING", setting.id, setting.settingKey,
                previous, setting.settingValue, "System setting updated");
        return new SettingView(setting.id, setting.settingKey, setting.settingValue, setting.description);
    }
}
