package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.ExamCourseRepository;
import com.erps.repository.MalpracticeCaseRepository;
import com.erps.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MalpracticeService {

    private final MalpracticeCaseRepository malpracticeCaseRepository;
    private final MarksRepository marksRepository;
    private final ExamCourseRepository examCourseRepository;
    private final AuditService auditService;

    public MalpracticeService(MalpracticeCaseRepository malpracticeCaseRepository,
                              MarksRepository marksRepository,
                              ExamCourseRepository examCourseRepository,
                              AuditService auditService) {
        this.malpracticeCaseRepository = malpracticeCaseRepository;
        this.marksRepository = marksRepository;
        this.examCourseRepository = examCourseRepository;
        this.auditService = auditService;
    }

    public List<MalpracticeCase> getAllCases() {
        return malpracticeCaseRepository.findAll();
    }

    public List<MalpracticeCase> getCasesForExam(Long examId) {
        return malpracticeCaseRepository.findByExamId(examId);
    }

    /**
     * FR9: CHIEF_SUPERINTENDENT flags student/course with Malpractice.
     * Automatically freezes the course result (HELD) in Marks.
     */
    @Transactional
    public MalpracticeCase flagMalpractice(Examination exam, Student student, Course course,
                                           String reason, String flaggedBy) {
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("A detailed incident report is required to flag malpractice.");
        }

        Optional<MalpracticeCase> existing = malpracticeCaseRepository.findByExamIdAndStudentIdAndCourseId(
                exam.getId(), student.getId(), course.getId());
        if (existing.isPresent()) {
            throw new IllegalStateException("A malpractice case is already registered for this student in this course.");
        }

        MalpracticeCase mc = new MalpracticeCase(exam, student, course, flaggedBy, reason);
        MalpracticeCase saved = malpracticeCaseRepository.save(mc);

        // Freeze marks
        Optional<ExamCourse> ecOpt = examCourseRepository.findByExamIdAndCourseId(exam.getId(), course.getId());
        if (ecOpt.isPresent()) {
            Optional<Marks> marksOpt = marksRepository.findByExamCourseIdAndStudentId(ecOpt.get().getId(), student.getId());
            if (marksOpt.isPresent()) {
                Marks m = marksOpt.get();
                m.setFlag(MarksFlag.MALPRACTICE);
                m.setUpdatedAt(LocalDateTime.now());
                marksRepository.save(m);
            }
        }

        auditService.logAction("FLAG_MALPRACTICE", "MalpracticeCase", saved.getId().toString(),
                null, String.format("Student %s flagged in %s by %s. Reason: %s",
                        student.getRegisterNo(), course.getCode(), flaggedBy, reason));
        return saved;
    }

    /**
     * FR9: Decision recorded by SUPER_ADMIN after Enquiry Committee hearing.
     */
    @Transactional
    public MalpracticeCase resolveCase(Long caseId, MalpracticeDecision decision, String penaltyNotes, String decidedBy) {
        MalpracticeCase mc = malpracticeCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Malpractice case not found"));

        mc.setDecision(decision);
        mc.setPenaltyNotes(penaltyNotes);
        mc.setDecidedBy(decidedBy);
        mc.setResolvedAt(LocalDateTime.now());

        // Update Marks flag accordingly
        Optional<ExamCourse> ecOpt = examCourseRepository.findByExamIdAndCourseId(mc.getExam().getId(), mc.getCourse().getId());
        if (ecOpt.isPresent()) {
            Optional<Marks> marksOpt = marksRepository.findByExamCourseIdAndStudentId(ecOpt.get().getId(), mc.getStudent().getId());
            if (marksOpt.isPresent()) {
                Marks m = marksOpt.get();
                if (decision == MalpracticeDecision.CLEARED) {
                    m.setFlag(MarksFlag.NONE); // unfreeze
                } else if (decision == MalpracticeDecision.PENALIZED) {
                    m.setFlag(MarksFlag.MALPRACTICE); // remains held / penalized
                }
                m.setUpdatedAt(LocalDateTime.now());
                marksRepository.save(m);
            }
        }

        MalpracticeCase saved = malpracticeCaseRepository.save(mc);
        auditService.logAction("RESOLVE_MALPRACTICE", "MalpracticeCase", caseId.toString(),
                "PENDING", String.format("Decision: %s by %s. Notes: %s", decision.name(), decidedBy, penaltyNotes));
        return saved;
    }
}
