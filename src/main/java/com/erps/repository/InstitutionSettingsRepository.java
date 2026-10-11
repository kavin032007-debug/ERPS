package com.erps.repository;

import com.erps.entity.InstitutionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionSettingsRepository extends JpaRepository<InstitutionSettings, Long> {
}
