package com.erps.repository;

import com.erps.entity.Result;
import com.erps.entity.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByExamIdAndStudentId(Long examId, Long studentId);
    List<Result> findByExamId(Long examId);
    List<Result> findByExamIdAndCourseId(Long examId, Long courseId);
    List<Result> findByStudentId(Long studentId);
    Optional<Result> findByExamIdAndStudentIdAndCourseId(Long examId, Long studentId, Long courseId);
    long countByExamIdAndStatus(Long examId, ResultStatus status);
}
