package com.erps.repository;

import com.erps.entity.DummyMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DummyMappingRepository extends JpaRepository<DummyMapping, Long> {
    Optional<DummyMapping> findByExamIdAndCourseIdAndDummyId(Long examId, Long courseId, String dummyId);
    Optional<DummyMapping> findByExamIdAndCourseIdAndStudentId(Long examId, Long courseId, Long studentId);
    List<DummyMapping> findByExamIdAndCourseId(Long examId, Long courseId);
    List<DummyMapping> findByExamId(Long examId);
}
