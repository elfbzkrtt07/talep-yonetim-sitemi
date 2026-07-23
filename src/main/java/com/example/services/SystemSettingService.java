package com.example.services;

import com.example.entities.SystemSetting;
import com.example.repositories.SystemSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    public boolean isFeatureEnabled(String key, boolean defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getBooleanValue)
                .orElse(defaultValue);
    }

    public void setFeatureEnabled(String key, boolean enabled) {
        repository.save(new SystemSetting(key, String.valueOf(enabled)));
    }
}