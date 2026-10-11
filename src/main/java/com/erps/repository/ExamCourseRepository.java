package com.erps.repository;

import com.erps.entity.ExamCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamCourseRepository extends JpaRepository<ExamCourse, Long> {
    List<ExamCourse> findByExamId(Long examId);
    Optional<ExamCourse> findByExamIdAndCourseId(Long examId, Long courseId);
}
