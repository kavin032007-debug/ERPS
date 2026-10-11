package com.erps.engine;

import com.erps.dto.CalculationDetails;
import com.erps.dto.CourseResultItem;
import com.erps.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradingEngineTest {

    private GradingEngine engine;
    private Course standardCourse;
    private List<GradeScale> regulationGradeScales;
    private GradingConfig standardConfig;

    @BeforeEach
    void setUp() {
        engine = new GradingEngine();

        standardCourse = new Course();
        standardCourse.setCode("CS101");
        standardCourse.setTitle("Data Structures");
        standardCourse.setCredits(BigDecimal.valueOf(4.0));
        standardCourse.setCiaMax(BigDecimal.valueOf(40.00));
        standardCourse.setSeeMax(BigDecimal.valueOf(60.00));
        standardCourse.setCiaWeight(BigDecimal.valueOf(40.00));
        standardCourse.setSeeWeight(BigDecimal.valueOf(60.00));

        Regulation reg = new Regulation("R2024", "Autonomous Regulation 2024", 2024);
        standardConfig = new GradingConfig(reg);

        regulationGradeScales = new ArrayList<>();
        regulationGradeScales.add(new GradeScale(reg, "S", BigDecimal.valueOf(91.00), BigDecimal.valueOf(100.00), 10));
        regulationGradeScales.add(new GradeScale(reg, "A+", BigDecimal.valueOf(81.00), BigDecimal.valueOf(90.99), 9));
        regulationGradeScales.add(new GradeScale(reg, "A", BigDecimal.valueOf(71.00), BigDecimal.valueOf(80.99), 8));
        regulationGradeScales.add(new GradeScale(reg, "B+", BigDecimal.valueOf(66.00), BigDecimal.valueOf(70.99), 7));
        regulationGradeScales.add(new GradeScale(reg, "B", BigDecimal.valueOf(61.00), BigDecimal.valueOf(65.99), 6));
        regulationGradeScales.add(new GradeScale(reg, "C+", BigDecimal.valueOf(56.00), BigDecimal.valueOf(60.99), 5));
        regulationGradeScales.add(new GradeScale(reg, "C", BigDecimal.valueOf(50.00), BigDecimal.valueOf(55.99), 4));
        regulationGradeScales.add(new GradeScale(reg, "U", BigDecimal.valueOf(0.00), BigDecimal.valueOf(49.99), 0));
    }

    @ParameterizedTest(name = "Total {0} marks should yield Grade {1} and GradePoint {2}")
    @CsvSource({
        "91.00, S, 10",
        "90.00, A+, 9",
        "81.00, A+, 9",
        "80.00, A, 8",
        "71.00, A, 8",
        "70.00, B+, 7",
        "66.00, B+, 7",
        "65.00, B, 6",
        "61.00, B, 6",
        "60.00, C+, 5",
        "56.00, C+, 5",
        "55.00, C, 4",
        "50.00, C, 4",
        "49.00, U, 0"
    })
    @DisplayName("Verify exact grade boundaries for absolute grading scale")
    void testGradeBoundaries(double totalMarksVal, String expectedGrade, int expectedGradePoint) {
        BigDecimal total = BigDecimal.valueOf(totalMarksVal);
        // Split total into CIA and SEE proportionally
        BigDecimal cia = total.multiply(BigDecimal.valueOf(0.40)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal see = total.subtract(cia).setScale(2, java.math.RoundingMode.HALF_UP);

        Marks marks = new Marks();
        marks.setCiaMarks(cia);
        marks.setSeeMarks(see);
        marks.setFlag(MarksFlag.NONE);

        CalculationDetails details = engine.computeCourseResult(standardCourse, marks, regulationGradeScales, standardConfig);

        assertEquals(expectedGrade, details.getGrade());
        assertEquals(expectedGradePoint, details.getGradePoint());
        if ("U".equals(expectedGrade)) {
            assertEquals(ResultStatus.FAIL, details.getStatus());
        } else {
            assertEquals(ResultStatus.PASS, details.getStatus());
        }
        assertNotNull(details.getExplanation());
        assertTrue(details.getExplanation().contains("Grade: " + expectedGrade));
    }

    @Test
    @DisplayName("Absent in exam gives Grade AB and Status ABSENT")
    void testAbsentStudent() {
        Marks marks = new Marks();
        marks.setCiaMarks(BigDecimal.valueOf(35.00));
        marks.setFlag(MarksFlag.ABSENT);

        CalculationDetails details = engine.computeCourseResult(standardCourse, marks, regulationGradeScales, standardConfig);

        assertEquals("AB", details.getGrade());
        assertEquals(0, details.getGradePoint());
        assertEquals(ResultStatus.ABSENT, details.getStatus());
    }

    @Test
    @DisplayName("Malpractice flags result as HELD and grade WH")
    void testMalpracticeStudent() {
        Marks marks = new Marks();
        marks.setCiaMarks(BigDecimal.valueOf(30.00));
        marks.setSeeMarks(BigDecimal.valueOf(45.00));
        marks.setFlag(MarksFlag.MALPRACTICE);

        CalculationDetails details = engine.computeCourseResult(standardCourse, marks, regulationGradeScales, standardConfig);

        assertEquals("WH", details.getGrade());
        assertEquals(0, details.getGradePoint());
        assertEquals(ResultStatus.HELD, details.getStatus());
    }

    @Test
    @DisplayName("Missing marks yield INCOMPLETE status")
    void testMissingMarks() {
        Marks marks = new Marks();
        marks.setCiaMarks(BigDecimal.valueOf(30.00));
        marks.setSeeMarks(null); // SEE missing

        CalculationDetails details = engine.computeCourseResult(standardCourse, marks, regulationGradeScales, standardConfig);

        assertEquals(ResultStatus.INCOMPLETE, details.getStatus());
    }

    @Test
    @DisplayName("Moderated marks take precedence over original SEE marks")
    void testModeration() {
        Marks marks = new Marks();
        marks.setCiaMarks(BigDecimal.valueOf(20.00));
        marks.setSeeMarks(BigDecimal.valueOf(28.00)); // Total 48 -> U
        marks.setModeratedMarks(BigDecimal.valueOf(30.00)); // Total 50 -> C

        CalculationDetails details = engine.computeCourseResult(standardCourse, marks, regulationGradeScales, standardConfig);

        assertEquals(BigDecimal.valueOf(50.00).setScale(2), details.getTotalMarks());
        assertEquals("C", details.getGrade());
        assertEquals(4, details.getGradePoint());
        assertEquals(ResultStatus.PASS, details.getStatus());
        assertTrue(details.getExplanation().contains("Moderated"));
    }

    @Test
    @DisplayName("GPA calculation matches standard weighted average")
    void testGpaCalculation() {
        // Course 1: 4 credits, Grade S (10) -> 40 points
        // Course 2: 3 credits, Grade A (8)  -> 24 points
        // Course 3: 3 credits, Grade B (6)  -> 18 points
        // Total points = 82, total credits = 10 -> GPA = 8.20
        List<CourseResultItem> items = List.of(
                new CourseResultItem("CS101", "DS", BigDecimal.valueOf(4.0), "S", 10, ResultStatus.PASS, BigDecimal.valueOf(95.00)),
                new CourseResultItem("CS102", "OOP", BigDecimal.valueOf(3.0), "A", 8, ResultStatus.PASS, BigDecimal.valueOf(75.00)),
                new CourseResultItem("CS103", "Maths", BigDecimal.valueOf(3.0), "B", 6, ResultStatus.PASS, BigDecimal.valueOf(62.00))
        );

        BigDecimal gpa = engine.computeGpa(items);
        assertEquals(BigDecimal.valueOf(8.20).setScale(2), gpa);
    }

    @Test
    @DisplayName("Zero-credit courses do not cause division-by-zero or affect GPA")
    void testZeroCreditCourses() {
        List<CourseResultItem> items = List.of(
                new CourseResultItem("CS101", "DS", BigDecimal.valueOf(4.0), "S", 10, ResultStatus.PASS, BigDecimal.valueOf(95.00)),
                new CourseResultItem("MC101", "Value Education", BigDecimal.ZERO, "S", 10, ResultStatus.PASS, BigDecimal.valueOf(92.00))
        );

        BigDecimal gpa = engine.computeGpa(items);
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), gpa);
    }
}
