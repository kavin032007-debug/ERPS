package com.erps.repository;

import com.erps.entity.RevaluationRequest;
import com.erps.entity.RevaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, Long> {
    List<RevaluationRequest> findByStudentId(Long studentId);
    List<RevaluationRequest> findByExamId(Long examId);
    List<RevaluationRequest> findByStatus(RevaluationStatus status);
    Optional<RevaluationRequest> findByStudentIdAndCourseIdAndExamId(Long studentId, Long courseId, Long examId);
    boolean existsByStudentIdAndCourseIdAndExamId(Long studentId, Long courseId, Long examId);
}
