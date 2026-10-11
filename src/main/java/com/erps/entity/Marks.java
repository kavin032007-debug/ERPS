package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_course_id", "student_id"})
})
public class Marks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_course_id", nullable = false)
    private ExamCourse examCourse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "cia_marks", precision = 5, scale = 2)
    private BigDecimal ciaMarks;

    @Column(name = "see_marks", precision = 5, scale = 2)
    private BigDecimal seeMarks;

    @Column(name = "moderated_marks", precision = 5, scale = 2)
    private BigDecimal moderatedMarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MarksFlag flag = MarksFlag.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MarksStatus status = MarksStatus.DRAFT;

    @Column(name = "entered_by")
    private String enteredBy;

    @Column(name = "verified_with_students")
    private Boolean verifiedWithStudents = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Marks() {}

    public Marks(ExamCourse examCourse, Student student, BigDecimal ciaMarks, BigDecimal seeMarks) {
        this.examCourse = examCourse;
        this.student = student;
        this.ciaMarks = ciaMarks;
        this.seeMarks = seeMarks;
        this.flag = MarksFlag.NONE;
        this.status = MarksStatus.DRAFT;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExamCourse getExamCourse() { return examCourse; }
    public void setExamCourse(ExamCourse examCourse) { this.examCourse = examCourse; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public BigDecimal getCiaMarks() { return ciaMarks; }
    public void setCiaMarks(BigDecimal ciaMarks) { this.ciaMarks = ciaMarks; }

    public BigDecimal getSeeMarks() { return seeMarks; }
    public void setSeeMarks(BigDecimal seeMarks) { this.seeMarks = seeMarks; }

    public BigDecimal getModeratedMarks() { return moderatedMarks; }
    public void setModeratedMarks(BigDecimal moderatedMarks) { this.moderatedMarks = moderatedMarks; }

    public MarksFlag getFlag() { return flag; }
    public void setFlag(MarksFlag flag) { this.flag = flag; }

    public MarksStatus getStatus() { return status; }
    public void setStatus(MarksStatus status) { this.status = status; }

    public String getEnteredBy() { return enteredBy; }
    public void setEnteredBy(String enteredBy) { this.enteredBy = enteredBy; }

    public Boolean getVerifiedWithStudents() { return verifiedWithStudents != null && verifiedWithStudents; }
    public void setVerifiedWithStudents(Boolean verifiedWithStudents) { this.verifiedWithStudents = verifiedWithStudents; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
