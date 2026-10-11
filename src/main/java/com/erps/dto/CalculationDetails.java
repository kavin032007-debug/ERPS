package com.erps.dto;

import com.erps.entity.ResultStatus;
import java.math.BigDecimal;

public class CalculationDetails {
    private BigDecimal ciaScaled;
    private BigDecimal seeScaled;
    private BigDecimal totalMarks;
    private String grade;
    private Integer gradePoint;
    private ResultStatus status;
    private String explanation;

    public CalculationDetails() {}

    public CalculationDetails(BigDecimal ciaScaled, BigDecimal seeScaled, BigDecimal totalMarks,
                              String grade, Integer gradePoint, ResultStatus status, String explanation) {
        this.ciaScaled = ciaScaled;
        this.seeScaled = seeScaled;
        this.totalMarks = totalMarks;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.status = status;
        this.explanation = explanation;
    }

    public BigDecimal getCiaScaled() { return ciaScaled; }
    public void setCiaScaled(BigDecimal ciaScaled) { this.ciaScaled = ciaScaled; }

    public BigDecimal getSeeScaled() { return seeScaled; }
    public void setSeeScaled(BigDecimal seeScaled) { this.seeScaled = seeScaled; }

    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Integer getGradePoint() { return gradePoint; }
    public void setGradePoint(Integer gradePoint) { this.gradePoint = gradePoint; }

    public ResultStatus getStatus() { return status; }
    public void setStatus(ResultStatus status) { this.status = status; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
