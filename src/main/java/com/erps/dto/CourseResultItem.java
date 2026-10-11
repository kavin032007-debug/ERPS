package com.erps.dto;

import com.erps.entity.ResultStatus;
import java.math.BigDecimal;

public class CourseResultItem {
    private String courseCode;
    private String courseTitle;
    private BigDecimal credits;
    private String grade;
    private Integer gradePoint;
    private ResultStatus status;
    private BigDecimal totalMarks;

    public CourseResultItem() {}

    public CourseResultItem(String courseCode, String courseTitle, BigDecimal credits,
                            String grade, Integer gradePoint, ResultStatus status, BigDecimal totalMarks) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.credits = credits;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.status = status;
        this.totalMarks = totalMarks;
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Integer getGradePoint() { return gradePoint; }
    public void setGradePoint(Integer gradePoint) { this.gradePoint = gradePoint; }

    public ResultStatus getStatus() { return status; }
    public void setStatus(ResultStatus status) { this.status = status; }

    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }

    public boolean isPassed() {
        return status == ResultStatus.PASS;
    }
}
