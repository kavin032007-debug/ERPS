package com.erps.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_courses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "course_id"})
})
public class ExamCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "cia_deadline")
    private LocalDateTime ciaDeadline;

    @Column(name = "see_deadline")
    private LocalDateTime seeDeadline;

    @Column(name = "internal_locked")
    private Boolean internalLocked = false;

    @Column(name = "internal_published_at")
    private LocalDateTime internalPublishedAt;

    public ExamCourse() {}

    public ExamCourse(Examination exam, Course course, LocalDateTime ciaDeadline, LocalDateTime seeDeadline) {
        this.exam = exam;
        this.course = course;
        this.ciaDeadline = ciaDeadline;
        this.seeDeadline = seeDeadline;
        this.internalLocked = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public Examination getExamination() { return exam; }
    public void setExamination(Examination exam) { this.exam = exam; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getCiaDeadline() { return ciaDeadline; }
    public void setCiaDeadline(LocalDateTime ciaDeadline) { this.ciaDeadline = ciaDeadline; }

    public LocalDateTime getSeeDeadline() { return seeDeadline; }
    public void setSeeDeadline(LocalDateTime seeDeadline) { this.seeDeadline = seeDeadline; }

    public Boolean getInternalLocked() { return internalLocked != null && internalLocked; }
    public void setInternalLocked(Boolean internalLocked) { this.internalLocked = internalLocked; }

    public LocalDateTime getInternalPublishedAt() { return internalPublishedAt; }
    public void setInternalPublishedAt(LocalDateTime internalPublishedAt) { this.internalPublishedAt = internalPublishedAt; }
}
