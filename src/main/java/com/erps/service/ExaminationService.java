package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.CourseRepository;
import com.erps.repository.ExamCourseRepository;
import com.erps.repository.ExaminationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExamCourseRepository examCourseRepository;
    private final CourseRepository courseRepository;
    private final AuditService auditService;

    public ExaminationService(ExaminationRepository examinationRepository,
                              ExamCourseRepository examCourseRepository,
                              CourseRepository courseRepository,
                              AuditService auditService) {
        this.examinationRepository = examinationRepository;
        this.examCourseRepository = examCourseRepository;
        this.courseRepository = courseRepository;
        this.auditService = auditService;
    }

    public List<Examination> getAllExaminations() {
        return examinationRepository.findAll();
    }

    public Optional<Examination> getExaminationById(Long id) {
        return examinationRepository.findById(id);
    }

    @Transactional
    public Examination createExamination(String name, String academicYear, Integer semester, Regulation regulation) {
        Examination exam = new Examination(name, academicYear, semester, regulation, ExamStatus.DRAFT);
        Examination saved = examinationRepository.save(exam);

        // Attach matching semester courses automatically to this examination
        List<Course> semesterCourses = courseRepository.findBySemester(semester);
        LocalDateTime defaultCiaDeadline = LocalDateTime.now().plusDays(14);
        LocalDateTime defaultSeeDeadline = LocalDateTime.now().plusDays(28);

        for (Course c : semesterCourses) {
            ExamCourse ec = new ExamCourse(saved, c, defaultCiaDeadline, defaultSeeDeadline);
            examCourseRepository.save(ec);
        }

        auditService.logAction("CREATE_EXAMINATION", "Examination", saved.getId().toString(),
                null, String.format("%s (%s, Sem %d)", name, academicYear, semester));
        return saved;
    }

    @Transactional
    public Examination updateStatus(Long examId, ExamStatus newStatus) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found"));
        ExamStatus oldStatus = exam.getStatus();
        exam.setStatus(newStatus);
        Examination saved = examinationRepository.save(exam);

        auditService.logAction("EXAM_STATUS_CHANGE", "Examination", examId.toString(),
                oldStatus.name(), newStatus.name());
        return saved;
    }

    @Transactional
    public Examination unlockExamination(Long examId, String reason, String authorizedBy) {
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("A valid reason of at least 5 characters is mandatory to unlock an examination.");
        }
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found"));

        ExamStatus oldStatus = exam.getStatus();
        exam.setStatus(ExamStatus.CIA_ENTRY); // Re-opens to entry mode
        Examination saved = examinationRepository.save(exam);

        auditService.logAction("UNLOCK_EXAMINATION", "Examination", examId.toString(),
                oldStatus.name(), String.format("Re-opened by %s. Reason: %s [ALERT sent to CoE/DCoE]", authorizedBy, reason));
        return saved;
    }

    public List<ExamCourse> getExamCourses(Long examId) {
        return examCourseRepository.findByExamId(examId);
    }

    public Optional<ExamCourse> getExamCourse(Long examId, Long courseId) {
        return examCourseRepository.findByExamIdAndCourseId(examId, courseId);
    }

    @Transactional
    public ExamCourse updateDeadlines(Long examCourseId, LocalDateTime ciaDeadline, LocalDateTime seeDeadline) {
        ExamCourse ec = examCourseRepository.findById(examCourseId)
                .orElseThrow(() -> new IllegalArgumentException("Exam course not found"));
        ec.setCiaDeadline(ciaDeadline);
        ec.setSeeDeadline(seeDeadline);
        ExamCourse saved = examCourseRepository.save(ec);

        auditService.logAction("UPDATE_DEADLINES", "ExamCourse", examCourseId.toString(),
                null, String.format("CIA: %s, SEE: %s", ciaDeadline, seeDeadline));
        return saved;
    }

    /**
     * Checks if faculty entry deadline has passed for a course in an exam.
     */
    public boolean isCiaEntryAllowed(ExamCourse ec) {
        if (ec.getExamination().getStatus() != ExamStatus.CIA_ENTRY && ec.getExamination().getStatus() != ExamStatus.DRAFT) {
            return false;
        }
        if (Boolean.TRUE.equals(ec.getInternalLocked())) {
            return false;
        }
        if (ec.getCiaDeadline() != null && LocalDateTime.now().isAfter(ec.getCiaDeadline())) {
            return false; // Deadline passed -> auto-revoked
        }
        return true;
    }

    public boolean isSeeEntryAllowed(ExamCourse ec) {
        if (ec.getExamination().getStatus() != ExamStatus.SEE_ENTRY) {
            return false;
        }
        if (ec.getSeeDeadline() != null && LocalDateTime.now().isAfter(ec.getSeeDeadline())) {
            return false; // Deadline passed -> auto-revoked
        }
        return true;
    }
}
