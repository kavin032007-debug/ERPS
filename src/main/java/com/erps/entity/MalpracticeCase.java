package com.erps.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "malpractice_cases", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "student_id", "course_id"})
})
public class MalpracticeCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "flagged_by", nullable = false)
    private String flaggedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MalpracticeDecision decision = MalpracticeDecision.PENDING;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "penalty_notes", columnDefinition = "TEXT")
    private String penaltyNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public MalpracticeCase() {}

    public MalpracticeCase(Examination exam, Student student, Course course, String flaggedBy, String reason) {
        this.exam = exam;
        this.student = student;
        this.course = course;
        this.flaggedBy = flaggedBy;
        this.reason = reason;
        this.decision = MalpracticeDecision.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getFlaggedBy() { return flaggedBy; }
    public void setFlaggedBy(String flaggedBy) { this.flaggedBy = flaggedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public MalpracticeDecision getDecision() { return decision; }
    public void setDecision(MalpracticeDecision decision) { this.decision = decision; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public String getPenaltyNotes() { return penaltyNotes; }
    public void setPenaltyNotes(String penaltyNotes) { this.penaltyNotes = penaltyNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
