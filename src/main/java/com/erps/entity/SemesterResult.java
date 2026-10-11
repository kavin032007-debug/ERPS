package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "semester_results", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "student_id"})
})
public class SemesterResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(precision = 4, scale = 2)
    private BigDecimal gpa;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "total_credits", precision = 5, scale = 1)
    private BigDecimal totalCredits;

    @Column(name = "earned_credits", precision = 5, scale = 1)
    private BigDecimal earnedCredits;

    @Column(name = "arrear_count")
    private Integer arrearCount = 0;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false, length = 50)
    private WorkflowStatus workflowStatus = WorkflowStatus.DRAFT;

    @Column(name = "result_reference", nullable = false, unique = true, length = 100)
    private String resultReference;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public SemesterResult() {}

    public SemesterResult(Examination exam, Student student, BigDecimal gpa, BigDecimal cgpa,
                          BigDecimal totalCredits, BigDecimal earnedCredits, Integer arrearCount,
                          Integer rankPosition, WorkflowStatus workflowStatus, String resultReference) {
        this.exam = exam;
        this.student = student;
        this.gpa = gpa;
        this.cgpa = cgpa;
        this.totalCredits = totalCredits;
        this.earnedCredits = earnedCredits;
        this.arrearCount = arrearCount != null ? arrearCount : 0;
        this.rankPosition = rankPosition;
        this.workflowStatus = workflowStatus != null ? workflowStatus : WorkflowStatus.DRAFT;
        this.resultReference = resultReference;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }

    public BigDecimal getEarnedCredits() { return earnedCredits; }
    public void setEarnedCredits(BigDecimal earnedCredits) { this.earnedCredits = earnedCredits; }

    public Integer getArrearCount() { return arrearCount; }
    public void setArrearCount(Integer arrearCount) { this.arrearCount = arrearCount; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public WorkflowStatus getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(WorkflowStatus workflowStatus) { this.workflowStatus = workflowStatus; }

    public String getResultReference() { return resultReference; }
    public void setResultReference(String resultReference) { this.resultReference = resultReference; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public boolean isPass() {
        if (arrearCount != null && arrearCount > 0) {
            return false;
        }
        if (totalCredits != null && totalCredits.compareTo(BigDecimal.ZERO) > 0) {
            return earnedCredits != null && earnedCredits.compareTo(totalCredits) == 0;
        }
        return true;
    }
}
