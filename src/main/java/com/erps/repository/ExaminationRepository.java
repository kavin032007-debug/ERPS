package com.erps.repository;

import com.erps.entity.ExamStatus;
import com.erps.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findByStatus(ExamStatus status);
    List<Examination> findByRegulationId(Long regulationId);
}
