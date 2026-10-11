package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "grade_scales", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"regulation_id", "grade"})
})
public class GradeScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "regulation_id", nullable = false)
    private Regulation regulation;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(name = "min_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal minMarks;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "grade_point", nullable = false)
    private Integer gradePoint;

    public GradeScale() {}

    public GradeScale(Regulation regulation, String grade, BigDecimal minMarks, BigDecimal maxMarks, Integer gradePoint) {
        this.regulation = regulation;
        this.grade = grade;
        this.minMarks = minMarks;
        this.maxMarks = maxMarks;
        this.gradePoint = gradePoint;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Regulation getRegulation() { return regulation; }
    public void setRegulation(Regulation regulation) { this.regulation = regulation; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public BigDecimal getMinMarks() { return minMarks; }
    public void setMinMarks(BigDecimal minMarks) { this.minMarks = minMarks; }

    public BigDecimal getMaxMarks() { return maxMarks; }
    public void setMaxMarks(BigDecimal maxMarks) { this.maxMarks = maxMarks; }

    public Integer getGradePoint() { return gradePoint; }
    public void setGradePoint(Integer gradePoint) { this.gradePoint = gradePoint; }
}
