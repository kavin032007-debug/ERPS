package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.Marks;
import com.erps.repository.MarksRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModerationService {

    private final MarksRepository marksRepository;
    private final AuditService auditService;

    public ModerationService(MarksRepository marksRepository, AuditService auditService) {
        this.marksRepository = marksRepository;
        this.auditService = auditService;
    }

    /**
     * Applies moderation (grace marks) to all eligible students in a course before RPB approval.
     * Both original SEE marks and moderated marks are preserved.
     */
    @Transactional
    public int applyCourseModeration(Long examCourseId, BigDecimal graceMarks, String reason, String authorizedBy) {
        if (graceMarks == null || graceMarks.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Grace marks must be greater than zero.");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("A valid moderation justification is required.");
        }

        List<Marks> marksList = marksRepository.findByExamCourseId(examCourseId);
        int count = 0;

        for (Marks m : marksList) {
            if (m.getSeeMarks() != null) {
                BigDecimal currentSee = m.getModeratedMarks() != null ? m.getModeratedMarks() : m.getSeeMarks();
                BigDecimal newSee = currentSee.add(graceMarks);
                BigDecimal max = m.getExamCourse().getCourse().getSeeMax();
                if (newSee.compareTo(max) > 0) {
                    newSee = max;
                }
                m.setModeratedMarks(newSee);
                m.setUpdatedAt(LocalDateTime.now());
                marksRepository.save(m);
                count++;
            }
        }

        auditService.logAction("APPLY_COURSE_MODERATION", "ExamCourse", examCourseId.toString(),
                null, String.format("+%s grace marks applied to %d candidates by %s. Reason: %s",
                        graceMarks.toPlainString(), count, authorizedBy, reason));
        return count;
    }
}
