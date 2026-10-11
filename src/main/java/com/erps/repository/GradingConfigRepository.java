package com.erps.repository;

import com.erps.entity.GradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradingConfigRepository extends JpaRepository<GradingConfig, Long> {
    Optional<GradingConfig> findByRegulationId(Long regulationId);
}
