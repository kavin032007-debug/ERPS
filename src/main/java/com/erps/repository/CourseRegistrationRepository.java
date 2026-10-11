package com.erps.repository;

import com.erps.entity.CourseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {
    List<CourseRegistration> findByStudentId(Long studentId);
    List<CourseRegistration> findByExamId(Long examId);
    List<CourseRegistration> findByExamIdAndCourseId(Long examId, Long courseId);
    List<CourseRegistration> findByExamIdAndStudentId(Long examId, Long studentId);
    Optional<CourseRegistration> findByStudentIdAndCourseIdAndExamId(Long studentId, Long courseId, Long examId);
    boolean existsByStudentIdAndCourseIdAndExamId(Long studentId, Long courseId, Long examId);
}
