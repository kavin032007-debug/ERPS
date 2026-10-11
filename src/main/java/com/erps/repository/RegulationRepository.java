package com.erps.repository;

import com.erps.entity.Regulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegulationRepository extends JpaRepository<Regulation, Long> {
    Optional<Regulation> findByCode(String code);
    boolean existsByCode(String code);
}
