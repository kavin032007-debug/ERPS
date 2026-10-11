package com.erps.repository;

import com.erps.entity.Marks;
import com.erps.entity.MarksStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarksRepository extends JpaRepository<Marks, Long> {
    Optional<Marks> findByExamCourseIdAndStudentId(Long examCourseId, Long studentId);
    List<Marks> findByExamCourseId(Long examCourseId);
    List<Marks> findByStudentId(Long studentId);
    List<Marks> findByStatus(MarksStatus status);
}
