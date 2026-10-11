package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.ExamCourseRepository;
import com.erps.repository.MarkAlterationRepository;
import com.erps.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    private final MarksRepository marksRepository;
    private final MarkAlterationRepository markAlterationRepository;
    private final ExamCourseRepository examCourseRepository;
    private final AuditService auditService;
    private final ExaminationService examinationService;

    public MarksService(MarksRepository marksRepository,
                        MarkAlterationRepository markAlterationRepository,
                        ExamCourseRepository examCourseRepository,
                        AuditService auditService,
                        ExaminationService examinationService) {
        this.marksRepository = marksRepository;
        this.markAlterationRepository = markAlterationRepository;
        this.examCourseRepository = examCourseRepository;
        this.auditService = auditService;
        this.examinationService = examinationService;
    }

    public List<Marks> getMarksForExamCourse(Long examCourseId) {
        return marksRepository.findByExamCourseId(examCourseId);
    }

    public Optional<Marks> getMarks(Long examCourseId, Long studentId) {
        return marksRepository.findByExamCourseIdAndStudentId(examCourseId, studentId);
    }

    /**
     * Saves or updates CIA marks.
     */
    @Transactional
    public Marks saveCiaMarks(ExamCourse examCourse, Student student, BigDecimal ciaMarks,
                              boolean isFinalSubmit, boolean verifiedWithStudents, String enteredBy, String userRole) {
        if (!examinationService.isCiaEntryAllowed(examCourse)) {
            throw new IllegalStateException("CIA marks entry is closed or the submission deadline has passed.");
        }
        if ("TECH_STAFF".equalsIgnoreCase(userRole)) {
            throw new SecurityException("TECH_STAFF is strictly prohibited from altering marks.");
        }

        BigDecimal max = examCourse.getCourse().getCiaMax();
        if (ciaMarks != null) {
            if (ciaMarks.compareTo(BigDecimal.ZERO) < 0 || ciaMarks.compareTo(max) > 0) {
                throw new IllegalArgumentException(String.format("CIA marks must be between 0 and %s", max));
            }
        }

        if (isFinalSubmit && !verifiedWithStudents) {
            throw new IllegalArgumentException("Faculty must confirm 'Verified with students' before final submission of CIA marks.");
        }

        Optional<Marks> existingOpt = marksRepository.findByExamCourseIdAndStudentId(examCourse.getId(), student.getId());
        Marks marks = existingOpt.orElseGet(() -> new Marks(examCourse, student, null, null));

        if ((marks.getStatus() == MarksStatus.SUBMITTED || marks.getStatus() == MarksStatus.FINAL)
                && marks.getCiaMarks() != null && ciaMarks != null && !ciaMarks.equals(marks.getCiaMarks())) {
            throw new IllegalStateException("CIA Marks have already been submitted. Use the Mark-Alteration request flow with justification.");
        }

        String oldVal = marks.getCiaMarks() != null ? marks.getCiaMarks().toPlainString() : "NULL";
        marks.setCiaMarks(ciaMarks);
        marks.setEnteredBy(enteredBy);
        marks.setVerifiedWithStudents(verifiedWithStudents);
        marks.setStatus(isFinalSubmit ? MarksStatus.SUBMITTED : MarksStatus.DRAFT);
        marks.setUpdatedAt(LocalDateTime.now());

        Marks saved = marksRepository.save(marks);
        auditService.logAction("ENTER_CIA_MARKS", "Marks", saved.getId().toString(),
                oldVal, ciaMarks != null ? ciaMarks.toPlainString() : "NULL");
        return saved;
    }

    /**
     * Locks and publishes internal marks for an exam course.
     */
    @Transactional
    public void lockAndPublishInternalMarks(Long examCourseId, String userEmail) {
        ExamCourse ec = examCourseRepository.findById(examCourseId)
                .orElseThrow(() -> new IllegalArgumentException("Exam course not found"));
        ec.setInternalLocked(true);
        ec.setInternalPublishedAt(LocalDateTime.now());
        examCourseRepository.save(ec);

        auditService.logAction("LOCK_INTERNAL_MARKS", "ExamCourse", examCourseId.toString(),
                "OPEN", "LOCKED_AND_PUBLISHED by " + userEmail);
    }

    /**
     * Saves or updates SEE marks (using dummy ID resolution or direct student).
     */
    @Transactional
    public Marks saveSeeMarks(ExamCourse examCourse, Student student, BigDecimal seeMarks,
                              MarksFlag flag, boolean isFinalSubmit, String enteredBy, String userRole) {
        if (!examinationService.isSeeEntryAllowed(examCourse)) {
            throw new IllegalStateException("SEE marks entry is closed or the submission deadline has passed.");
        }
        if ("TECH_STAFF".equalsIgnoreCase(userRole)) {
            throw new SecurityException("TECH_STAFF is strictly prohibited from altering marks.");
        }

        BigDecimal max = examCourse.getCourse().getSeeMax();
        if (flag == MarksFlag.NONE && seeMarks != null) {
            if (seeMarks.compareTo(BigDecimal.ZERO) < 0 || seeMarks.compareTo(max) > 0) {
                throw new IllegalArgumentException(String.format("SEE marks must be between 0 and %s", max));
            }
        }

        Optional<Marks> existingOpt = marksRepository.findByExamCourseIdAndStudentId(examCourse.getId(), student.getId());
        Marks marks = existingOpt.orElseGet(() -> new Marks(examCourse, student, null, null));

        // If SEE marks were already SUBMITTED or FINAL, any alteration requires FR8 sign-off queue!
        if ((marks.getStatus() == MarksStatus.SUBMITTED || marks.getStatus() == MarksStatus.FINAL)
                && marks.getSeeMarks() != null && seeMarks != null && !seeMarks.equals(marks.getSeeMarks())) {
            throw new IllegalStateException("Marks have already been submitted. Use the Mark-Alteration request flow with justification.");
        }

        String oldVal = marks.getSeeMarks() != null ? marks.getSeeMarks().toPlainString() : "NULL";
        marks.setSeeMarks(seeMarks);
        marks.setFlag(flag != null ? flag : MarksFlag.NONE);
        marks.setEnteredBy(enteredBy);
        marks.setStatus(isFinalSubmit ? MarksStatus.SUBMITTED : MarksStatus.DRAFT);
        marks.setUpdatedAt(LocalDateTime.now());

        Marks saved = marksRepository.save(marks);
        auditService.logAction("ENTER_SEE_MARKS", "Marks", saved.getId().toString(),
                oldVal, seeMarks != null ? seeMarks.toPlainString() : (flag != null ? flag.name() : "NULL"));
        return saved;
    }

    /**
     * FR8: Request a mark alteration after initial submission.
     */
    @Transactional
    public MarkAlteration requestMarkAlteration(Long marksId, BigDecimal newValue, String reason, String requestedBy) {
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("A valid justification reason (at least 5 characters) is required.");
        }
        Marks marks = marksRepository.findById(marksId)
                .orElseThrow(() -> new IllegalArgumentException("Marks record not found"));

        BigDecimal oldVal = marks.getModeratedMarks() != null ? marks.getModeratedMarks() : marks.getSeeMarks();
        MarkAlteration alteration = new MarkAlteration(marks, oldVal, newValue, reason, requestedBy);
        MarkAlteration saved = markAlterationRepository.save(alteration);

        marks.setStatus(MarksStatus.PENDING_SIGNOFF);
        marksRepository.save(marks);

        auditService.logAction("REQUEST_MARK_ALTERATION", "MarkAlteration", saved.getId().toString(),
                oldVal != null ? oldVal.toPlainString() : "NULL",
                String.format("New: %s; Reason: %s [ALERT sent to CoE/DCoE]", newValue, reason));
        return saved;
    }

    public List<MarkAlteration> getPendingAlterations() {
        return markAlterationRepository.findByStatus(AlterationStatus.PENDING);
    }

    /**
     * FR8: SUPER_ADMIN signs off (approves/rejects) a mark alteration.
     */
    @Transactional
    public MarkAlteration signOffAlteration(Long alterationId, boolean approve, String comments, String signedBy) {
        if (comments == null || comments.trim().isEmpty()) {
            throw new IllegalArgumentException("Sign-off comment is mandatory.");
        }
        MarkAlteration alteration = markAlterationRepository.findById(alterationId)
                .orElseThrow(() -> new IllegalArgumentException("Alteration request not found"));

        if (alteration.getStatus() != AlterationStatus.PENDING) {
            throw new IllegalStateException("Alteration request is already processed.");
        }

        alteration.setReviewedAt(LocalDateTime.now());
        alteration.setSignedBy(signedBy);
        alteration.setComments(comments);

        Marks marks = alteration.getMarks();

        if (approve) {
            alteration.setStatus(AlterationStatus.APPROVED);
            marks.setSeeMarks(alteration.getNewValue());
            marks.setStatus(MarksStatus.FINAL);
            marks.setUpdatedAt(LocalDateTime.now());
            marksRepository.save(marks);
        } else {
            alteration.setStatus(AlterationStatus.REJECTED);
            marks.setStatus(MarksStatus.FINAL);
            marksRepository.save(marks);
        }

        MarkAlteration saved = markAlterationRepository.save(alteration);
        auditService.logAction(approve ? "APPROVE_MARK_ALTERATION" : "REJECT_MARK_ALTERATION",
                "MarkAlteration", alterationId.toString(),
                alteration.getOldValue() != null ? alteration.getOldValue().toPlainString() : "NULL",
                String.format("Approved: %b, SignedBy: %s, Comments: %s", approve, signedBy, comments));
        return saved;
    }
}
