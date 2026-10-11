package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.GradingConfigRepository;
import com.erps.repository.RevaluationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RevaluationService {

    private final RevaluationRequestRepository revaluationRequestRepository;
    private final GradingConfigRepository gradingConfigRepository;
    private final MarksService marksService;
    private final ResultService resultService;
    private final AuditService auditService;

    public RevaluationService(RevaluationRequestRepository revaluationRequestRepository,
                              GradingConfigRepository gradingConfigRepository,
                              MarksService marksService,
                              ResultService resultService,
                              AuditService auditService) {
        this.revaluationRequestRepository = revaluationRequestRepository;
        this.gradingConfigRepository = gradingConfigRepository;
        this.marksService = marksService;
        this.resultService = resultService;
        this.auditService = auditService;
    }

    public List<RevaluationRequest> getAllRequests() {
        return revaluationRequestRepository.findAll();
    }

    public List<RevaluationRequest> getRequestsForStudent(Long studentId) {
        return revaluationRequestRepository.findByStudentId(studentId);
    }

    public List<RevaluationRequest> getRequestsForExam(Long examId) {
        return revaluationRequestRepository.findByExamId(examId);
    }

    /**
     * FR11: Student initiates revaluation request.
     */
    @Transactional
    public RevaluationRequest submitRequest(Student student, Course course, Examination exam,
                                            String reason, boolean requestPhotocopy, BigDecimal currentMarks) {
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("Revaluation can only be requested for published examinations.");
        }

        // Verify revaluation window
        GradingConfig config = exam.getRegulation() != null
                ? gradingConfigRepository.findByRegulationId(exam.getRegulation().getId()).orElse(null)
                : null;
        int windowDays = config != null && config.getRevalWindowDays() != null ? config.getRevalWindowDays() : 15;

        // Check window (from publication or created_at)
        LocalDateTime windowEnd = exam.getCreatedAt().plusDays(windowDays + 30); // lenient for demo
        if (LocalDateTime.now().isAfter(windowEnd)) {
            throw new IllegalStateException("The revaluation submission window has expired.");
        }

        if (revaluationRequestRepository.existsByStudentIdAndCourseIdAndExamId(student.getId(), course.getId(), exam.getId())) {
            throw new IllegalStateException("A revaluation request has already been filed for this course.");
        }

        RevaluationRequest request = new RevaluationRequest(student, course, exam, reason, false, currentMarks);
        request.setPhotocopyRequested(requestPhotocopy);
        RevaluationRequest saved = revaluationRequestRepository.save(request);

        auditService.logAction("SUBMIT_REVALUATION_REQUEST", "RevaluationRequest", saved.getId().toString(),
                null, String.format("Student %s for %s (%s)", student.getRegisterNo(), course.getCode(), reason));
        return saved;
    }

    /**
     * TECH_STAFF records fee payment flag and marks registration complete.
     */
    @Transactional
    public void recordFeePaymentAndRegister(Long requestId, boolean feePaid, String staffName) {
        RevaluationRequest req = revaluationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        req.setFeePaidFlag(feePaid);
        req.setStatus(RevaluationStatus.REGISTERED);
        revaluationRequestRepository.save(req);

        auditService.logAction("PROCESS_REVALUATION_REGISTRATION", "RevaluationRequest", requestId.toString(),
                "REQUESTED", "REGISTERED with feePaid=" + feePaid + " by " + staffName);
    }

    /**
     * Update photocopy workflow status.
     */
    @Transactional
    public void markPhotocopyAvailable(Long requestId) {
        RevaluationRequest req = revaluationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        req.setPhotocopyAvailableAt(LocalDateTime.now());
        revaluationRequestRepository.save(req);
    }

    @Transactional
    public void markPhotocopyReceived(Long requestId) {
        RevaluationRequest req = revaluationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        req.setPhotocopyReceivedAt(LocalDateTime.now());
        revaluationRequestRepository.save(req);
    }

    /**
     * Revaluation valuation result entered, queued for mark alteration sign-off.
     */
    @Transactional
    public void completeRevaluationValuation(Long requestId, BigDecimal revisedMarks, Long marksId, String examiner) {
        RevaluationRequest req = revaluationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        req.setNewMarks(revisedMarks);
        req.setStatus(RevaluationStatus.IN_VALUATION);
        revaluationRequestRepository.save(req);

        // Queue mark alteration sign-off
        marksService.requestMarkAlteration(marksId, revisedMarks,
                "Revaluation valuation completed for request #" + requestId + " by " + examiner, examiner);

        req.setStatus(RevaluationStatus.COMPLETED);
        req.setResolvedAt(LocalDateTime.now());
        revaluationRequestRepository.save(req);

        // Trigger result recalculation
        resultService.calculateResultsForExam(req.getExam().getId());

        auditService.logAction("COMPLETE_REVALUATION", "RevaluationRequest", requestId.toString(),
                req.getOldMarks() != null ? req.getOldMarks().toPlainString() : "NULL",
                "New: " + revisedMarks.toPlainString());
    }

    public boolean isRevaluationOverdue(RevaluationRequest req, int slaDays) {
        if (req.getStatus() != RevaluationStatus.COMPLETED) {
            LocalDateTime target = req.getCreatedAt().plusDays(slaDays);
            return LocalDateTime.now().isAfter(target);
        }
        return false;
    }
}
