package com.erps.repository;

import com.erps.entity.FacultyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultyAssignmentRepository extends JpaRepository<FacultyAssignment, Long> {
    List<FacultyAssignment> findByFacultyId(Long facultyId);
    List<FacultyAssignment> findByCourseId(Long courseId);
    boolean existsByFacultyIdAndCourseId(Long facultyId, Long courseId);
}
