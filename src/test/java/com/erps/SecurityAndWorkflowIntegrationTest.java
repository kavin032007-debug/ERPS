package com.erps;

import com.erps.audit.AuditService;
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
class SecurityAndWorkflowIntegrationTest {

    @Autowired private MasterDataService masterDataService;
    @Autowired private ExaminationService examinationService;
    @Autowired private MarksService marksService;
    @Autowired private WorkflowService workflowService;
    @Autowired private MalpracticeService malpracticeService;
    @Autowired private ResultService resultService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProgrammeRepository programmeRepository;
    @Autowired private RegulationRepository regulationRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ExamCourseRepository examCourseRepository;
    @Autowired private SemesterResultRepository semesterResultRepository;
    @Autowired private AuditService auditService;

    private Student testStudent;
    private Course testCourse;
    private Examination testExam;
    private ExamCourse testExamCourse;

    @BeforeEach
    void setupTestData() {
        Department dept = departmentRepository.save(new Department("MECH", "Mechanical Engineering"));
        Programme prog = programmeRepository.save(new Programme("BTECH-MECH", "B.Tech Mechanical", dept));
        Regulation reg = masterDataService.saveRegulation(new Regulation("R2024-TEST", "Autonomous Regulation 2024 Test", 2024));

        testStudent = masterDataService.saveStudent(new Student(null, "24ME001", "Ramesh Kumar", dept, prog, reg, 2024, 3, "ramesh@institution.edu"), "Pass@123");
        testCourse = masterDataService.saveCourse(new Course("ME201", "Thermodynamics", BigDecimal.valueOf(4.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));

        testExam = examinationService.createExamination("Nov 2025 Test Exam", "2025-2026", 3, reg);
        testExamCourse = examCourseRepository.findByExamIdAndCourseId(testExam.getId(), testCourse.getId()).orElseThrow();
        masterDataService.registerStudentCourse(testStudent, testCourse, testExam, "Coordinator");
    }

    @Test
    @DisplayName("Security: TECH_STAFF is strictly prohibited from altering marks")
    void testTechStaffCannotAlterMarks() {
        examinationService.updateStatus(testExam.getId(), ExamStatus.CIA_ENTRY);

        assertThrows(SecurityException.class, () -> {
            marksService.saveCiaMarks(testExamCourse, testStudent, BigDecimal.valueOf(35.0), false, false, "staff@institution.edu", "TECH_STAFF");
        }, "TECH_STAFF role must be blocked by server-side security from entering marks");
    }

    @Test
    @DisplayName("Security: Edit access auto-revokes when faculty submission deadline passes")
    void testDeadlineRevocation() {
        examinationService.updateStatus(testExam.getId(), ExamStatus.CIA_ENTRY);
        // Set deadline to past
        examinationService.updateDeadlines(testExamCourse.getId(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));

        assertThrows(IllegalStateException.class, () -> {
            marksService.saveCiaMarks(testExamCourse, testStudent, BigDecimal.valueOf(35.0), false, false, "fac@institution.edu", "FACULTY");
        }, "Marks entry must be rejected after deadline passes");
    }

    @Test
    @DisplayName("Workflow: Publication is strictly blocked without prior RPB approval")
    void testPublicationBlockedWithoutRpbApproval() {
        // Enter marks
        examinationService.updateStatus(testExam.getId(), ExamStatus.CIA_ENTRY);
        marksService.saveCiaMarks(testExamCourse, testStudent, BigDecimal.valueOf(35.0), true, true, "fac@institution.edu", "FACULTY");

        examinationService.updateStatus(testExam.getId(), ExamStatus.SEE_ENTRY);
        marksService.saveSeeMarks(testExamCourse, testStudent, BigDecimal.valueOf(50.0), MarksFlag.NONE, true, "fac@institution.edu", "FACULTY");

        // Calculate and submit results
        workflowService.submitResults(testExam.getId(), "CoE");
        workflowService.verifyResults(testExam.getId(), "DCoE");

        // Attempt direct publication without RPB approval
        assertThrows(IllegalStateException.class, () -> {
            workflowService.publishResults(testExam.getId(), "CoE");
        }, "Publication must be blocked if results are not RPB_APPROVED");
    }

    @Test
    @DisplayName("Discipline: Malpractice flag freezes result as HELD and blocks passing")
    void testMalpracticeHold() {
        examinationService.updateStatus(testExam.getId(), ExamStatus.CIA_ENTRY);
        marksService.saveCiaMarks(testExamCourse, testStudent, BigDecimal.valueOf(35.0), true, true, "fac@institution.edu", "FACULTY");

        examinationService.updateStatus(testExam.getId(), ExamStatus.SEE_ENTRY);
        marksService.saveSeeMarks(testExamCourse, testStudent, BigDecimal.valueOf(50.0), MarksFlag.NONE, true, "fac@institution.edu", "FACULTY");

        // Flag malpractice
        malpracticeService.flagMalpractice(testExam, testStudent, testCourse, "Unauthorized cheat sheet", "Chief Supt");

        // Calculate results
        resultService.calculateResultsForExam(testExam.getId());

        SemesterResult semRes = semesterResultRepository.findByExamIdAndStudentId(testExam.getId(), testStudent.getId()).orElseThrow();
        assertEquals(0, semRes.getEarnedCredits().intValue(), "Held course credits cannot be earned");
        assertFalse(semRes.isPass(), "Student with malpractice hold cannot pass overall");
    }

    @Test
    @DisplayName("Audit: Post-submission mark alteration requires sign-off queue and does not apply silently")
    void testMarkAlterationQueue() {
        examinationService.updateStatus(testExam.getId(), ExamStatus.SEE_ENTRY);
        Marks submitted = marksService.saveSeeMarks(testExamCourse, testStudent, BigDecimal.valueOf(40.0), MarksFlag.NONE, true, "fac@institution.edu", "FACULTY");

        // Attempting to overwrite submitted marks directly throws exception
        assertThrows(IllegalStateException.class, () -> {
            marksService.saveSeeMarks(testExamCourse, testStudent, BigDecimal.valueOf(45.0), MarksFlag.NONE, true, "fac@institution.edu", "FACULTY");
        });

        // Request alteration via FR8 queue
        MarkAlteration alt = marksService.requestMarkAlteration(submitted.getId(), BigDecimal.valueOf(45.0), "Valuation re-totalling error in Q5", "fac@institution.edu");
        assertEquals(AlterationStatus.PENDING, alt.getStatus());

        // CoE approves alteration
        MarkAlteration approved = marksService.signOffAlteration(alt.getId(), true, "Approved after verifying physical answer script", "CoE");
        assertEquals(AlterationStatus.APPROVED, approved.getStatus());
        assertEquals(BigDecimal.valueOf(45.0), approved.getMarks().getSeeMarks());
    }
}
