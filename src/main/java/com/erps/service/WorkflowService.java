package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.ExaminationRepository;
import com.erps.repository.ResultRepository;
import com.erps.repository.RpbMeetingRepository;
import com.erps.repository.SemesterResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowService {

    private final ExaminationRepository examinationRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final ResultRepository resultRepository;
    private final RpbMeetingRepository rpbMeetingRepository;
    private final AuditService auditService;
    private final ResultService resultService;

    public WorkflowService(ExaminationRepository examinationRepository,
                           SemesterResultRepository semesterResultRepository,
                           ResultRepository resultRepository,
                           RpbMeetingRepository rpbMeetingRepository,
                           AuditService auditService,
                           ResultService resultService) {
        this.examinationRepository = examinationRepository;
        this.semesterResultRepository = semesterResultRepository;
        this.resultRepository = resultRepository;
        this.rpbMeetingRepository = rpbMeetingRepository;
        this.auditService = auditService;
        this.resultService = resultService;
    }

    /**
     * Step 1: Submit results after calculation (DRAFT -> SUBMITTED)
     */
    @Transactional
    public void submitResults(Long examId, String submittedBy) {
        // Run fresh calculation
        resultService.calculateResultsForExam(examId);

        List<SemesterResult> results = semesterResultRepository.findByExamId(examId);
        for (SemesterResult sr : results) {
            sr.setWorkflowStatus(WorkflowStatus.SUBMITTED);
            semesterResultRepository.save(sr);
        }

        auditService.logAction("SUBMIT_RESULTS", "Examination", examId.toString(),
                "DRAFT", "SUBMITTED by " + submittedBy);
    }

    /**
     * Step 2: Verification by DCoE / CoE (SUBMITTED -> VERIFIED)
     */
    @Transactional
    public void verifyResults(Long examId, String verifiedBy) {
        List<SemesterResult> results = semesterResultRepository.findByExamId(examId);
        if (results.isEmpty()) {
            throw new IllegalStateException("No results found to verify.");
        }

        // Check for any INCOMPLETE results
        long incomplete = resultRepository.countByExamIdAndStatus(examId, ResultStatus.INCOMPLETE);
        if (incomplete > 0) {
            throw new IllegalStateException(String.format("Cannot verify results: %d course marks entries are INCOMPLETE.", incomplete));
        }

        for (SemesterResult sr : results) {
            sr.setWorkflowStatus(WorkflowStatus.VERIFIED);
            semesterResultRepository.save(sr);
        }

        auditService.logAction("VERIFY_RESULTS", "Examination", examId.toString(),
                "SUBMITTED", "VERIFIED by " + verifiedBy);
    }

    /**
     * Step 3: Result Passing Board (RPB) Approval (VERIFIED -> RPB_APPROVED)
     * Chaired by Principal, CoE member secretary, optional University nominee.
     */
    @Transactional
    public RpbMeeting approveRpb(Long examId, LocalDateTime meetingDate, String members,
                                 String universityNominee, String minutesRef, String signedBy) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found"));

        List<SemesterResult> results = semesterResultRepository.findByExamId(examId);
        if (results.isEmpty()) {
            throw new IllegalStateException("No results available for RPB approval.");
        }

        for (SemesterResult sr : results) {
            if (sr.getWorkflowStatus() != WorkflowStatus.VERIFIED && sr.getWorkflowStatus() != WorkflowStatus.SUBMITTED) {
                throw new IllegalStateException("Results must be VERIFIED prior to RPB approval.");
            }
            sr.setWorkflowStatus(WorkflowStatus.RPB_APPROVED);
            semesterResultRepository.save(sr);
        }

        RpbMeeting meeting = new RpbMeeting(exam, meetingDate, members, universityNominee, minutesRef, signedBy);
        RpbMeeting saved = rpbMeetingRepository.save(meeting);

        auditService.logAction("RPB_APPROVAL", "Examination", examId.toString(),
                "VERIFIED", String.format("RPB_APPROVED by Principal / Chairman %s. Minutes: %s", signedBy, minutesRef));
        return saved;
    }

    /**
     * Step 4: Publication to Students (RPB_APPROVED -> PUBLISHED)
     * Strictly requires RPB_APPROVED. Atomic single transaction per exam.
     */
    @Transactional
    public void publishResults(Long examId, String publishedBy) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found"));

        List<SemesterResult> results = semesterResultRepository.findByExamId(examId);
        if (results.isEmpty()) {
            throw new IllegalStateException("No results to publish.");
        }

        for (SemesterResult sr : results) {
            if (sr.getWorkflowStatus() != WorkflowStatus.RPB_APPROVED) {
                throw new IllegalStateException("Publication blocked: Results have not been approved by the Result Passing Board (RPB).");
            }
            sr.setWorkflowStatus(WorkflowStatus.PUBLISHED);
            sr.setPublishedAt(LocalDateTime.now());
            semesterResultRepository.save(sr);
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        examinationRepository.save(exam);

        auditService.logAction("PUBLISH_RESULTS", "Examination", examId.toString(),
                "RPB_APPROVED", "PUBLISHED by " + publishedBy);
    }

    /**
     * Checks if publication target SLA (3 days from RPB approval) is overdue.
     */
    public boolean isPublicationOverdue(Long examId, int slaDays) {
        return rpbMeetingRepository.findByExamId(examId).map(meeting -> {
            Examination exam = meeting.getExam();
            if (exam.getStatus() != ExamStatus.PUBLISHED) {
                LocalDateTime target = meeting.getMeetingDate().plusDays(slaDays);
                return LocalDateTime.now().isAfter(target);
            }
            return false;
        }).orElse(false);
    }
}
