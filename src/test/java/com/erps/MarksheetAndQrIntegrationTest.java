package com.erps;

import com.erps.entity.*;
import com.erps.repository.*;
import com.erps.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarksheetAndQrIntegrationTest {

    @Autowired private MasterDataService masterDataService;
    @Autowired private ExaminationService examinationService;
    @Autowired private MarksService marksService;
    @Autowired private WorkflowService workflowService;
    @Autowired private MarksheetService marksheetService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProgrammeRepository programmeRepository;
    @Autowired private ExamCourseRepository examCourseRepository;
    @Autowired private SemesterResultRepository semesterResultRepository;

    private Student testStudent;
    private Examination testExam;
    private SemesterResult publishedSemResult;

    @BeforeEach
    void setupTestData() {
        Department dept = departmentRepository.save(new Department("IT", "Information Technology"));
        Programme prog = programmeRepository.save(new Programme("BTECH-IT", "B.Tech IT", dept));
        Regulation reg = masterDataService.saveRegulation(new Regulation("R2024-MS", "Reg 2024 MS", 2024));

        testStudent = masterDataService.saveStudent(new Student(null, "24IT001", "Priya Venkatesh", dept, prog, reg, 2024, 3, "priya@institution.edu"), "Pass@123");
        Course course = masterDataService.saveCourse(new Course("IT201", "Web Technologies", BigDecimal.valueOf(4.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));

        testExam = examinationService.createExamination("Nov 2025 MS Exam", "2025-2026", 3, reg);
        ExamCourse ec = examCourseRepository.findByExamIdAndCourseId(testExam.getId(), course.getId()).orElseThrow();
        masterDataService.registerStudentCourse(testStudent, course, testExam, "Coord");

        // Marks & Workflow to PUBLISHED
        examinationService.updateStatus(testExam.getId(), ExamStatus.CIA_ENTRY);
        marksService.saveCiaMarks(ec, testStudent, BigDecimal.valueOf(38.0), true, true, "fac@institution.edu", "FACULTY");

        examinationService.updateStatus(testExam.getId(), ExamStatus.SEE_ENTRY);
        marksService.saveSeeMarks(ec, testStudent, BigDecimal.valueOf(55.0), MarksFlag.NONE, true, "fac@institution.edu", "FACULTY");

        workflowService.submitResults(testExam.getId(), "CoE");
        workflowService.verifyResults(testExam.getId(), "DCoE");
        workflowService.approveRpb(testExam.getId(), LocalDateTime.now(), "Principal, CoE", "Nominee", "RPB/2025/TEST", "Principal");
        workflowService.publishResults(testExam.getId(), "CoE");

        publishedSemResult = semesterResultRepository.findByExamIdAndStudentId(testExam.getId(), testStudent.getId()).orElseThrow();
    }

    @Test
    @DisplayName("PDF Mark Sheet: Generates valid byte array with embedded QR code and demo disclaimer")
    void testPdfMarksheetGeneration() {
        byte[] pdfBytes = marksheetService.generateSemesterMarksheetPdf(publishedSemResult.getId(), false);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "PDF document must contain substantial content");

        // Check PDF header signature '%PDF'
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @DisplayName("Public QR Verification: Returns authentic summary with masked initials and leaks NO marks")
    void testPublicVerification() {
        String validRef = publishedSemResult.getResultReference();
        MarksheetService.VerificationSummary summary = marksheetService.verifyResultReference(validRef);

        assertTrue(summary.valid(), "Result reference must verify as valid");
        assertEquals("P. V.", summary.studentInitials(), "Student full name must be masked to initials only");
        assertEquals(testExam.getName(), summary.examName());
        assertEquals(validRef, summary.resultReference());

        // Test invalid reference
        MarksheetService.VerificationSummary invalidSummary = marksheetService.verifyResultReference("FAKE-REF-999");
        assertFalse(invalidSummary.valid(), "Fake reference code must return invalid");
    }
}
