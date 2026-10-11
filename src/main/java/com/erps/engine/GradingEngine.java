package com.erps.engine;

import com.erps.dto.CalculationDetails;
import com.erps.dto.CourseResultItem;
import com.erps.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Pure Java Grading Engine for calculating course marks, letter grades,
 * GPA, CGPA, and pass/fail statuses according to autonomous institution regulations.
 */
@Component
public class GradingEngine {

    /**
     * Computes course level result breakdown.
     */
    public CalculationDetails computeCourseResult(Course course, Marks marks,
                                                  List<GradeScale> gradeScales,
                                                  GradingConfig config) {
        if (marks == null) {
            return new CalculationDetails(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "N/A", 0, ResultStatus.INCOMPLETE,
                    "Marks entry not initiated."
            );
        }

        // Check Malpractice hold
        if (marks.getFlag() == MarksFlag.MALPRACTICE) {
            return new CalculationDetails(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "WH", 0, ResultStatus.HELD,
                    "Result withheld pending Malpractice / Examination Enquiry Committee decision."
            );
        }

        // Check Absent
        if (marks.getFlag() == MarksFlag.ABSENT) {
            return new CalculationDetails(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "AB", 0, ResultStatus.ABSENT,
                    "Candidate marked Absent (AB) in Semester End Examination."
            );
        }

        // Check Missing / Incomplete marks
        if (marks.getCiaMarks() == null || (marks.getSeeMarks() == null && marks.getModeratedMarks() == null)) {
            return new CalculationDetails(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "INC", 0, ResultStatus.INCOMPLETE,
                    "Marks entry incomplete (CIA or SEE marks missing)."
            );
        }

        BigDecimal cia = marks.getCiaMarks();
        BigDecimal rawSee = marks.getModeratedMarks() != null ? marks.getModeratedMarks() : marks.getSeeMarks();

        // Safe scaling calculation
        BigDecimal ciaMax = course.getCiaMax() != null && course.getCiaMax().compareTo(BigDecimal.ZERO) > 0
                ? course.getCiaMax() : BigDecimal.valueOf(40.00);
        BigDecimal seeMax = course.getSeeMax() != null && course.getSeeMax().compareTo(BigDecimal.ZERO) > 0
                ? course.getSeeMax() : BigDecimal.valueOf(60.00);
        BigDecimal ciaWeight = course.getCiaWeight() != null ? course.getCiaWeight() : BigDecimal.valueOf(40.00);
        BigDecimal seeWeight = course.getSeeWeight() != null ? course.getSeeWeight() : BigDecimal.valueOf(60.00);

        BigDecimal ciaScaled = cia.multiply(ciaWeight).divide(ciaMax, 2, RoundingMode.HALF_UP);
        BigDecimal seeScaled = rawSee.multiply(seeWeight).divide(seeMax, 2, RoundingMode.HALF_UP);
        BigDecimal totalMarks = ciaScaled.add(seeScaled).setScale(2, RoundingMode.HALF_UP);

        // Check Minimum SEE marks requirement if enabled
        boolean minSeeViolated = false;
        BigDecimal requiredMinSee = null;
        if (config != null && Boolean.TRUE.equals(config.getMinSeeMarksEnabled())) {
            requiredMinSee = config.getDefaultMinSeeMarks();
        }
        if (course.getMinSeeMarks() != null) {
            requiredMinSee = course.getMinSeeMarks();
        }

        if (requiredMinSee != null && rawSee.compareTo(requiredMinSee) < 0) {
            minSeeViolated = true;
        }

        // Grade scale matching
        String matchedGrade = "U";
        int matchedGradePoint = 0;
        ResultStatus status = ResultStatus.FAIL;

        if (minSeeViolated) {
            matchedGrade = "U";
            matchedGradePoint = 0;
            status = ResultStatus.FAIL;
        } else if (gradeScales != null && !gradeScales.isEmpty()) {
            for (GradeScale scale : gradeScales) {
                if (totalMarks.compareTo(scale.getMinMarks()) >= 0 && totalMarks.compareTo(scale.getMaxMarks()) <= 0) {
                    matchedGrade = scale.getGrade();
                    matchedGradePoint = scale.getGradePoint();
                    status = "U".equalsIgnoreCase(matchedGrade) ? ResultStatus.FAIL : ResultStatus.PASS;
                    break;
                }
            }
        } else {
            // Default Absolute Grading Scale (Autonomous College)
            if (totalMarks.compareTo(BigDecimal.valueOf(91.00)) >= 0) {
                matchedGrade = "S"; matchedGradePoint = 10; status = ResultStatus.PASS;
            } else if (totalMarks.compareTo(BigDecimal.valueOf(81.00)) >= 0) {
                matchedGrade = "A+"; matchedGradePoint = 9; status = ResultStatus.PASS;
            } else if (totalMarks.compareTo(BigDecimal.valueOf(71.00)) >= 0) {
                matchedGrade = "A"; matchedGradePoint = 8; status = ResultStatus.PASS;
            } else if (totalMarks.compareTo(BigDecimal.valueOf(66.00)) >= 0) {
                matchedGrade = "B+"; matchedGradePoint = 7; status = ResultStatus.PASS;
            } else if (totalMarks.compareTo(BigDecimal.valueOf(61.00)) >= 0) {
                matchedGrade = "B"; matchedGradePoint = 6; status = ResultStatus.PASS;
            } else if (totalMarks.compareTo(BigDecimal.valueOf(56.00)) >= 0) {
                matchedGrade = "C+"; matchedGradePoint = 5; status = ResultStatus.PASS;
            } else if (totalMarks.compareTo(BigDecimal.valueOf(50.00)) >= 0) {
                matchedGrade = "C"; matchedGradePoint = 4; status = ResultStatus.PASS;
            } else {
                matchedGrade = "U"; matchedGradePoint = 0; status = ResultStatus.FAIL;
            }
        }

        StringBuilder explanation = new StringBuilder();
        explanation.append(String.format("CIA: %s/%s (wt %s%%) -> %s; ",
                cia.toPlainString(), ciaMax.toPlainString(), ciaWeight.toPlainString(), ciaScaled.toPlainString()));
        if (marks.getModeratedMarks() != null) {
            explanation.append(String.format("SEE (Moderated): %s/%s (orig %s) (wt %s%%) -> %s; ",
                    rawSee.toPlainString(), seeMax.toPlainString(), marks.getSeeMarks().toPlainString(), seeWeight.toPlainString(), seeScaled.toPlainString()));
        } else {
            explanation.append(String.format("SEE: %s/%s (wt %s%%) -> %s; ",
                    rawSee.toPlainString(), seeMax.toPlainString(), seeWeight.toPlainString(), seeScaled.toPlainString()));
        }
        explanation.append(String.format("Total: %s/100; Grade: %s (GP: %d); Status: %s",
                totalMarks.toPlainString(), matchedGrade, matchedGradePoint, status.name()));

        if (minSeeViolated) {
            explanation.append(String.format(" [Failed min SEE threshold: %s < %s]",
                    rawSee.toPlainString(), requiredMinSee.toPlainString()));
        }

        return new CalculationDetails(ciaScaled, seeScaled, totalMarks, matchedGrade,
                matchedGradePoint, status, explanation.toString());
    }

    /**
     * Calculates GPA = sum(credit * gradePoint) / sum(credits) for courses with credits > 0.
     * Division by zero safe.
     */
    public BigDecimal computeGpa(List<CourseResultItem> courseResults) {
        if (courseResults == null || courseResults.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (CourseResultItem item : courseResults) {
            if (item.getCredits() != null && item.getCredits().compareTo(BigDecimal.ZERO) > 0) {
                totalCredits = totalCredits.add(item.getCredits());
                int gp = item.getGradePoint() != null ? item.getGradePoint() : 0;
                totalPoints = totalPoints.add(item.getCredits().multiply(BigDecimal.valueOf(gp)));
            }
        }

        if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return totalPoints.divide(totalCredits, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates CGPA across latest attempts of all courses.
     */
    public BigDecimal computeCgpa(List<CourseResultItem> allAttemptedCourses) {
        if (allAttemptedCourses == null || allAttemptedCourses.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Map to keep the latest attempt by course code
        Map<String, CourseResultItem> latestAttempts = new LinkedHashMap<>();
        for (CourseResultItem item : allAttemptedCourses) {
            if (item.getCourseCode() != null) {
                latestAttempts.put(item.getCourseCode(), item);
            }
        }

        return computeGpa(new ArrayList<>(latestAttempts.values()));
    }
}
