package com.erps.repository;

import com.erps.entity.GradeScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeScaleRepository extends JpaRepository<GradeScale, Long> {
    List<GradeScale> findByRegulationIdOrderByMinMarksDesc(Long regulationId);
    Optional<GradeScale> findByRegulationIdAndGrade(Long regulationId, String grade);
    void deleteByRegulationId(Long regulationId);
}
