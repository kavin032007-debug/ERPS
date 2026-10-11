package com.erps.repository;

import com.erps.entity.SemesterResult;
import com.erps.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterResultRepository extends JpaRepository<SemesterResult, Long> {
    Optional<SemesterResult> findByExamIdAndStudentId(Long examId, Long studentId);
    Optional<SemesterResult> findByResultReference(String resultReference);
    List<SemesterResult> findByExamId(Long examId);
    List<SemesterResult> findByStudentId(Long studentId);
    List<SemesterResult> findByStudentIdAndWorkflowStatus(Long studentId, WorkflowStatus workflowStatus);
    List<SemesterResult> findByExamIdAndWorkflowStatus(Long examId, WorkflowStatus workflowStatus);
    long countByExamIdAndWorkflowStatus(Long examId, WorkflowStatus workflowStatus);
}
