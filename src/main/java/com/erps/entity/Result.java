package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "results", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "student_id", "course_id"})
})
public class Result {

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

    @Column(name = "cia_marks", precision = 5, scale = 2)
    private BigDecimal ciaMarks;

    @Column(name = "see_marks", precision = 5, scale = 2)
    private BigDecimal seeMarks;

    @Column(precision = 5, scale = 2)
    private BigDecimal total;

    @Column(length = 10)
    private String grade;

    @Column(name = "grade_point")
    private Integer gradePoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResultStatus status;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    public Result() {}

    public Result(Examination exam, Student student, Course course, BigDecimal ciaMarks,
                  BigDecimal seeMarks, BigDecimal total, String grade, Integer gradePoint,
                  ResultStatus status, String explanation) {
        this.exam = exam;
        this.student = student;
        this.course = course;
        this.ciaMarks = ciaMarks;
        this.seeMarks = seeMarks;
        this.total = total;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.status = status;
        this.explanation = explanation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public BigDecimal getCiaMarks() { return ciaMarks; }
    public void setCiaMarks(BigDecimal ciaMarks) { this.ciaMarks = ciaMarks; }

    public BigDecimal getSeeMarks() { return seeMarks; }
    public void setSeeMarks(BigDecimal seeMarks) { this.seeMarks = seeMarks; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Integer getGradePoint() { return gradePoint; }
    public void setGradePoint(Integer gradePoint) { this.gradePoint = gradePoint; }

    public ResultStatus getStatus() { return status; }
    public void setStatus(ResultStatus status) { this.status = status; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
