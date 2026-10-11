package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revaluation_requests")
public class RevaluationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "fee_paid_flag")
    private Boolean feePaidFlag = false;

    @Column(name = "photocopy_requested")
    private Boolean photocopyRequested = false;

    @Column(name = "photocopy_available_at")
    private LocalDateTime photocopyAvailableAt;

    @Column(name = "photocopy_received_at")
    private LocalDateTime photocopyReceivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RevaluationStatus status = RevaluationStatus.REQUESTED;

    @Column(name = "old_marks", precision = 5, scale = 2)
    private BigDecimal oldMarks;

    @Column(name = "new_marks", precision = 5, scale = 2)
    private BigDecimal newMarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public RevaluationRequest() {}

    public RevaluationRequest(Student student, Course course, Examination exam, String reason,
                              Boolean feePaidFlag, BigDecimal oldMarks) {
        this.student = student;
        this.course = course;
        this.exam = exam;
        this.reason = reason;
        this.feePaidFlag = feePaidFlag != null ? feePaidFlag : false;
        this.oldMarks = oldMarks;
        this.status = RevaluationStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getFeePaidFlag() { return feePaidFlag != null && feePaidFlag; }
    public void setFeePaidFlag(Boolean feePaidFlag) { this.feePaidFlag = feePaidFlag; }

    public Boolean getPhotocopyRequested() { return photocopyRequested != null && photocopyRequested; }
    public void setPhotocopyRequested(Boolean photocopyRequested) { this.photocopyRequested = photocopyRequested; }

    public LocalDateTime getPhotocopyAvailableAt() { return photocopyAvailableAt; }
    public void setPhotocopyAvailableAt(LocalDateTime photocopyAvailableAt) { this.photocopyAvailableAt = photocopyAvailableAt; }

    public LocalDateTime getPhotocopyReceivedAt() { return photocopyReceivedAt; }
    public void setPhotocopyReceivedAt(LocalDateTime photocopyReceivedAt) { this.photocopyReceivedAt = photocopyReceivedAt; }

    public RevaluationStatus getStatus() { return status; }
    public void setStatus(RevaluationStatus status) { this.status = status; }

    public BigDecimal getOldMarks() { return oldMarks; }
    public void setOldMarks(BigDecimal oldMarks) { this.oldMarks = oldMarks; }

    public BigDecimal getNewMarks() { return newMarks; }
    public void setNewMarks(BigDecimal newMarks) { this.newMarks = newMarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
