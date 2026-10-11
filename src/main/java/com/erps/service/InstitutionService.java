package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.InstitutionSettings;
import com.erps.repository.InstitutionSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {

    private final InstitutionSettingsRepository repository;
    private final AuditService auditService;

    @Value("${app.institution.name:Your Institution Name}")
    private String defaultName;

    @Value("${app.institution.address:Campus Road, Knowledge City, State - 600001}")
    private String defaultAddress;

    @Value("${app.institution.university:Affiliating State University}")
    private String defaultUniversity;

    public InstitutionService(InstitutionSettingsRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public InstitutionSettings getSettings() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            InstitutionSettings settings = new InstitutionSettings(defaultName, defaultAddress, "logo.png", defaultUniversity);
            return repository.save(settings);
        });
    }

    @Transactional
    public InstitutionSettings updateSettings(String name, String address, String university, String logoRef) {
        InstitutionSettings settings = getSettings();
        String oldVal = String.format("name=%s, university=%s", settings.getName(), settings.getUniversityName());
        settings.setName(name);
        settings.setAddress(address);
        settings.setUniversityName(university);
        settings.setLogoRef(logoRef);
        InstitutionSettings saved = repository.save(settings);
        auditService.logAction("UPDATE_INSTITUTION_SETTINGS", "InstitutionSettings", saved.getId().toString(),
                oldVal, String.format("name=%s, university=%s", name, university));
        return saved;
    }
}
