package com.erps;

import com.erps.entity.*;
import com.erps.repository.*;
import com.erps.service.ExaminationService;
import com.erps.service.MasterDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MasterDataAndRegistrationTest {

    @Autowired private MasterDataService masterDataService;
    @Autowired private ExaminationService examinationService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProgrammeRepository programmeRepository;

    private Department dept;
    private Programme prog;
    private Regulation reg;

    @BeforeEach
    void setUp() {
        dept = departmentRepository.save(new Department("CIVIL", "Civil Engineering"));
        prog = programmeRepository.save(new Programme("BTECH-CIVIL", "B.Tech Civil", dept));
        reg = masterDataService.saveRegulation(new Regulation("R2024-CIVIL", "Reg 2024 Civil", 2024));
    }

    @Test
    @DisplayName("Duplicate course registration for same student and exam is rejected with clear message")
    void testDuplicateCourseRegistrationRejected() {
        Student s = masterDataService.saveStudent(new Student(null, "24CE001", "Anand", dept, prog, reg, 2024, 3, "anand@institution.edu"), "Pass@123");
        Course c = masterDataService.saveCourse(new Course("CE201", "Surveying", BigDecimal.valueOf(3.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));
        Examination exam = examinationService.createExamination("Nov 2025 Civil", "2025-2026", 3, reg);

        // First registration succeeds
        masterDataService.registerStudentCourse(s, c, exam, "Coord");

        // Second registration must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            masterDataService.registerStudentCourse(s, c, exam, "Coord");
        });
        assertTrue(ex.getMessage().contains("already registered"));
    }
}
