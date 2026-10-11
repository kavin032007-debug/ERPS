package com.erps.repository;

import com.erps.entity.MalpracticeCase;
import com.erps.entity.MalpracticeDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MalpracticeCaseRepository extends JpaRepository<MalpracticeCase, Long> {
    Optional<MalpracticeCase> findByExamIdAndStudentIdAndCourseId(Long examId, Long studentId, Long courseId);
    List<MalpracticeCase> findByExamId(Long examId);
    List<MalpracticeCase> findByStudentId(Long studentId);
    List<MalpracticeCase> findByDecision(MalpracticeDecision decision);
    boolean existsByExamIdAndStudentIdAndCourseIdAndDecision(Long examId, Long studentId, Long courseId, MalpracticeDecision decision);
}
