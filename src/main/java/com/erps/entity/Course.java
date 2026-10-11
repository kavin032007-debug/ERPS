package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal credits;

    @Column(nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CourseType type = CourseType.THEORY;

    @Column(name = "cia_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal ciaMax = BigDecimal.valueOf(40.00);

    @Column(name = "see_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal seeMax = BigDecimal.valueOf(60.00);

    @Column(name = "cia_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal ciaWeight = BigDecimal.valueOf(40.00);

    @Column(name = "see_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal seeWeight = BigDecimal.valueOf(60.00);

    @Column(name = "min_see_marks", precision = 5, scale = 2)
    private BigDecimal minSeeMarks;

    public Course() {}

    public Course(String code, String title, BigDecimal credits, Integer semester,
                  CourseType type, BigDecimal ciaMax, BigDecimal seeMax,
                  BigDecimal ciaWeight, BigDecimal seeWeight, BigDecimal minSeeMarks) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.semester = semester;
        this.type = type != null ? type : CourseType.THEORY;
        this.ciaMax = ciaMax != null ? ciaMax : BigDecimal.valueOf(40.00);
        this.seeMax = seeMax != null ? seeMax : BigDecimal.valueOf(60.00);
        this.ciaWeight = ciaWeight != null ? ciaWeight : BigDecimal.valueOf(40.00);
        this.seeWeight = seeWeight != null ? seeWeight : BigDecimal.valueOf(60.00);
        this.minSeeMarks = minSeeMarks;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public CourseType getType() { return type; }
    public void setType(CourseType type) { this.type = type; }

    public BigDecimal getCiaMax() { return ciaMax; }
    public void setCiaMax(BigDecimal ciaMax) { this.ciaMax = ciaMax; }

    public BigDecimal getSeeMax() { return seeMax; }
    public void setSeeMax(BigDecimal seeMax) { this.seeMax = seeMax; }

    public BigDecimal getCiaWeight() { return ciaWeight; }
    public void setCiaWeight(BigDecimal ciaWeight) { this.ciaWeight = ciaWeight; }

    public BigDecimal getSeeWeight() { return seeWeight; }
    public void setSeeWeight(BigDecimal seeWeight) { this.seeWeight = seeWeight; }

    public BigDecimal getMinSeeMarks() { return minSeeMarks; }
    public void setMinSeeMarks(BigDecimal minSeeMarks) { this.minSeeMarks = minSeeMarks; }
}
