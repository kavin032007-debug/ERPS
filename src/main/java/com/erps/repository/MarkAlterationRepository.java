package com.erps.repository;

import com.erps.entity.AlterationStatus;
import com.erps.entity.MarkAlteration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkAlterationRepository extends JpaRepository<MarkAlteration, Long> {
    List<MarkAlteration> findByStatus(AlterationStatus status);
    List<MarkAlteration> findByMarksId(Long marksId);
    long countByStatus(AlterationStatus status);
}
