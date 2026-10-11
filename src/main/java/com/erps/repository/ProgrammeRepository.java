package com.erps.repository;

import com.erps.entity.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Long> {
    Optional<Programme> findByCode(String code);
    List<Programme> findByDepartmentId(Long departmentId);
    boolean existsByCode(String code);
}
