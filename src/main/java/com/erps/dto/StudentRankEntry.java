package com.erps.dto;

import java.math.BigDecimal;

public class StudentRankEntry {
    private Long studentId;
    private String registerNo;
    private String studentName;
    private BigDecimal gpa;
    private BigDecimal totalMarks;
    private int arrearCount;
    private Integer rank;

    public StudentRankEntry() {}

    public StudentRankEntry(Long studentId, String registerNo, String studentName,
                            BigDecimal gpa, BigDecimal totalMarks, int arrearCount) {
        this.studentId = studentId;
        this.registerNo = registerNo;
        this.studentName = studentName;
        this.gpa = gpa != null ? gpa : BigDecimal.ZERO;
        this.totalMarks = totalMarks != null ? totalMarks : BigDecimal.ZERO;
        this.arrearCount = arrearCount;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }

    public int getArrearCount() { return arrearCount; }
    public void setArrearCount(int arrearCount) { this.arrearCount = arrearCount; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
}
