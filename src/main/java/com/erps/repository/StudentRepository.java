package com.erps.repository;

import com.erps.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRegisterNo(String registerNo);
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByEmail(String email);
    List<Student> findByDepartmentId(Long departmentId);
    List<Student> findByBatchYear(Integer batchYear);
    List<Student> findByRegulationId(Long regulationId);
    boolean existsByRegisterNo(String registerNo);
}
