package com.erps.config;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.*;
import com.erps.service.MasterDataService;
import com.erps.service.ResultService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final InstitutionSettingsRepository institutionSettingsRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final RegulationRepository regulationRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamCourseRepository examCourseRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final MarksRepository marksRepository;
    private final MarkAlterationRepository markAlterationRepository;
    private final MalpracticeCaseRepository malpracticeCaseRepository;
    private final RevaluationRequestRepository revaluationRequestRepository;
    private final RpbMeetingRepository rpbMeetingRepository;
    private final GradeScaleRepository gradeScaleRepository;
    private final GradingConfigRepository gradingConfigRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final MasterDataService masterDataService;
    private final ResultService resultService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.seed-data:false}")
    private boolean seedDataEnabled;

    @Value("${app.institution.name:Your Institution Name}")
    private String institutionName;

    @Value("${app.institution.address:Campus Road, Knowledge City, State - 600001}")
    private String institutionAddress;

    @Value("${app.institution.university:Affiliating State University}")
    private String universityName;

    public DataInitializer(UserRepository userRepository,
                           InstitutionSettingsRepository institutionSettingsRepository,
                           DepartmentRepository departmentRepository,
                           ProgrammeRepository programmeRepository,
                           RegulationRepository regulationRepository,
                           SemesterRepository semesterRepository,
                           CourseRepository courseRepository,
                           StudentRepository studentRepository,
                           FacultyRepository facultyRepository,
                           FacultyAssignmentRepository facultyAssignmentRepository,
                           ExaminationRepository examinationRepository,
                           ExamCourseRepository examCourseRepository,
                           CourseRegistrationRepository courseRegistrationRepository,
                           MarksRepository marksRepository,
                           MarkAlterationRepository markAlterationRepository,
                           MalpracticeCaseRepository malpracticeCaseRepository,
                           RevaluationRequestRepository revaluationRequestRepository,
                           RpbMeetingRepository rpbMeetingRepository,
                           GradeScaleRepository gradeScaleRepository,
                           GradingConfigRepository gradingConfigRepository,
                           SemesterResultRepository semesterResultRepository,
                           MasterDataService masterDataService,
                           ResultService resultService,
                           AuditService auditService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.institutionSettingsRepository = institutionSettingsRepository;
        this.departmentRepository = departmentRepository;
        this.programmeRepository = programmeRepository;
        this.regulationRepository = regulationRepository;
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.examinationRepository = examinationRepository;
        this.examCourseRepository = examCourseRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.marksRepository = marksRepository;
        this.markAlterationRepository = markAlterationRepository;
        this.malpracticeCaseRepository = malpracticeCaseRepository;
        this.revaluationRequestRepository = revaluationRequestRepository;
        this.rpbMeetingRepository = rpbMeetingRepository;
        this.gradeScaleRepository = gradeScaleRepository;
        this.gradingConfigRepository = gradingConfigRepository;
        this.semesterResultRepository = semesterResultRepository;
        this.masterDataService = masterDataService;
        this.resultService = resultService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDataEnabled && userRepository.count() > 0) {
            return;
        }

        if (institutionSettingsRepository.count() == 0) {
            institutionSettingsRepository.save(new InstitutionSettings(
                    institutionName, institutionAddress, "logo.png", universityName
            ));
        }

        if (userRepository.count() > 0) {
            return;
        }

        // 1. Seed System Staff & Leadership Users
        User superAdmin = createUser("admin@institution.edu", "Admin@123", "Controller of Examinations", Role.SUPER_ADMIN);
        User principal = createUser("principal@institution.edu", "Principal@123", "Dr. A. Principal (RPB Chairman)", Role.PRINCIPAL);
        User techStaff = createUser("techstaff@institution.edu", "Staff@123", "Exam Office Tech Assistant", Role.TECH_STAFF);
        User coordinator = createUser("coordinator@institution.edu", "Coord@123", "Prof. CSE Coordinator", Role.EXAM_CELL_COORDINATOR);
        User chief = createUser("chief@institution.edu", "Chief@123", "Chief Superintendent (Exam Hall)", Role.CHIEF_SUPERINTENDENT);

        // 2. Departments & Programmes
        Department cseDept = departmentRepository.save(new Department("CSE", "Computer Science & Engineering"));
        Department eceDept = departmentRepository.save(new Department("ECE", "Electronics & Communication Engineering"));

        Programme btechCse = programmeRepository.save(new Programme("BTECH-CSE", "B.Tech Computer Science and Engineering", cseDept));
        Programme btechEce = programmeRepository.save(new Programme("BTECH-ECE", "B.Tech Electronics and Communication Engineering", eceDept));

        // 3. Regulations & Scales
        Regulation r2024 = masterDataService.saveRegulation(new Regulation("R2024", "Autonomous Absolute Regulation 2024", 2024));
        Regulation r2022 = masterDataService.saveRegulation(new Regulation("R2022", "Prior Autonomous Regulation 2022", 2022));

        // 4. Semesters
        Semester sem3 = semesterRepository.save(new Semester(3, "2025-2026"));
        Semester sem4 = semesterRepository.save(new Semester(4, "2025-2026"));

        // 5. Faculty
        Faculty fac1 = createFaculty("faculty1@institution.edu", "Fac1@123", "Dr. Alan Turing", cseDept, "Professor");
        Faculty fac2 = createFaculty("faculty2@institution.edu", "Fac2@123", "Dr. Ada Lovelace", cseDept, "Associate Professor");
        Faculty fac3 = createFaculty("faculty3@institution.edu", "Fac3@123", "Prof. Claude Shannon", cseDept, "Assistant Professor");

        // 6. Courses (6 core courses)
        Course c1 = courseRepository.save(new Course("CS201", "Data Structures & Algorithms", BigDecimal.valueOf(4.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));
        Course c2 = courseRepository.save(new Course("CS202", "Object Oriented Software Engineering", BigDecimal.valueOf(3.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));
        Course c3 = courseRepository.save(new Course("CS203", "Database Management Systems", BigDecimal.valueOf(4.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));
        Course c4 = courseRepository.save(new Course("CS204", "Discrete Mathematics", BigDecimal.valueOf(4.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));
        Course c5 = courseRepository.save(new Course("CS205", "Computer Networks", BigDecimal.valueOf(3.0), 3, CourseType.THEORY, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));
        Course c6 = courseRepository.save(new Course("CS206", "DBMS Laboratory", BigDecimal.valueOf(2.0), 3, CourseType.PRACTICAL, BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(60.0), null));

        List<Course> courses = List.of(c1, c2, c3, c4, c5, c6);

        // Assign Faculty
        facultyAssignmentRepository.save(new FacultyAssignment(fac1, c1, "2025-2026"));
        facultyAssignmentRepository.save(new FacultyAssignment(fac1, c2, "2025-2026"));
        facultyAssignmentRepository.save(new FacultyAssignment(fac2, c3, "2025-2026"));
        facultyAssignmentRepository.save(new FacultyAssignment(fac2, c6, "2025-2026"));
        facultyAssignmentRepository.save(new FacultyAssignment(fac3, c4, "2025-2026"));
        facultyAssignmentRepository.save(new FacultyAssignment(fac3, c5, "2025-2026"));

        // 7. Examination
        Examination exam = new Examination("Nov/Dec 2025 End Semester Examination", "2025-2026", 3, r2024, ExamStatus.PUBLISHED);
        exam = examinationRepository.save(exam);

        List<ExamCourse> examCourses = new ArrayList<>();
        LocalDateTime deadline = LocalDateTime.now().plusDays(30);
        for (Course c : courses) {
            ExamCourse ec = new ExamCourse(exam, c, deadline, deadline);
            ec.setInternalLocked(true);
            ec.setInternalPublishedAt(LocalDateTime.now().minusDays(10));
            examCourses.add(examCourseRepository.save(ec));
        }

        // 8. Seed ~30 Students
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            String regNo = String.format("24CS%03d", i);
            String email = String.format("student%02d@institution.edu", i);
            String name = getStudentName(i);
            Regulation reg = (i <= 26) ? r2024 : r2022; // 4 students from older regulation
            int batchYear = (i <= 26) ? 2024 : 2022;

            User studentUser = createUser(email, "Student@123", name, Role.STUDENT);
            Student s = new Student(studentUser, regNo, name, cseDept, btechCse, reg, batchYear, 3, email);
            students.add(studentRepository.save(s));
        }

        // Register students and assign marks
        Marks targetMarksForSignoff = null;

        for (int sIdx = 0; sIdx < students.size(); sIdx++) {
            Student s = students.get(sIdx);
            for (int cIdx = 0; cIdx < courses.size(); cIdx++) {
                Course c = courses.get(cIdx);
                ExamCourse ec = examCourses.get(cIdx);

                courseRegistrationRepository.save(new CourseRegistration(s, c, exam, "VERIFIED", coordinator.getEmail()));

                // Marks generation logic
                BigDecimal cia;
                BigDecimal see;
                MarksFlag flag = MarksFlag.NONE;

                if (sIdx == 0 || sIdx == 1) {
                    // Rank tie toppers: identical marks (95, 92, 94, 91, 93, 96) -> S grades, identical GPA & total
                    cia = BigDecimal.valueOf(38.00);
                    see = BigDecimal.valueOf(56.00 + (cIdx % 2));
                } else if (sIdx == 15 && cIdx == 2) {
                    // Student 16 absent in DBMS
                    cia = BigDecimal.valueOf(30.00);
                    see = BigDecimal.ZERO;
                    flag = MarksFlag.ABSENT;
                } else if (sIdx == 18 && cIdx == 0) {
                    // Student 19 malpractice in Data Structures
                    cia = BigDecimal.valueOf(28.00);
                    see = BigDecimal.valueOf(35.00);
                    flag = MarksFlag.MALPRACTICE;
                } else if (sIdx >= 24 && cIdx == 3) {
                    // A few fails (Discrete Maths)
                    cia = BigDecimal.valueOf(18.00);
                    see = BigDecimal.valueOf(25.00); // Total 43 -> U
                } else {
                    // Realistic bell curve marks
                    double base = 65.0 + ((sIdx * 3 + cIdx * 7) % 30);
                    cia = BigDecimal.valueOf(base * 0.4).setScale(2, java.math.RoundingMode.HALF_UP);
                    see = BigDecimal.valueOf(base * 0.6).setScale(2, java.math.RoundingMode.HALF_UP);
                }

                Marks m = new Marks(ec, s, cia, see);
                m.setFlag(flag);
                m.setStatus(MarksStatus.FINAL);
                m.setVerifiedWithStudents(true);
                m.setEnteredBy("fac1@institution.edu");
                Marks savedMarks = marksRepository.save(m);

                if (sIdx == 5 && cIdx == 0) {
                    targetMarksForSignoff = savedMarks;
                }
            }
        }

        // Process Results
        resultService.calculateResultsForExam(exam.getId());

        // RPB Approval
        rpbMeetingRepository.save(new RpbMeeting(
                exam, LocalDateTime.now().minusDays(2),
                "Dr. A. Principal (Chairman), Dr. CoE (Member Sec), Dr. HOD-CSE, Dr. HOD-ECE",
                "Dr. University Nominee, Dean of Affiliated Colleges",
                "RPB/2025/M02", principal.getEmail()
        ));

        // Mark as PUBLISHED across semester results
        List<SemesterResult> semRes = semesterResultRepository.findByExamId(exam.getId());
        for (SemesterResult sr : semRes) {
            sr.setWorkflowStatus(WorkflowStatus.PUBLISHED);
            sr.setPublishedAt(LocalDateTime.now().minusDays(1));
            semesterResultRepository.save(sr);
        }

        // FR8 Pending mark-alteration sign-off
        if (targetMarksForSignoff != null) {
            targetMarksForSignoff.setStatus(MarksStatus.PENDING_SIGNOFF);
            marksRepository.save(targetMarksForSignoff);
            MarkAlteration alt = new MarkAlteration(
                    targetMarksForSignoff,
                    targetMarksForSignoff.getSeeMarks(),
                    targetMarksForSignoff.getSeeMarks().add(BigDecimal.valueOf(5.00)),
                    "Re-totalling correction requested by course coordinator Dr. Turing",
                    fac1.getEmail()
            );
            markAlterationRepository.save(alt);
        }

        // FR9 Malpractice Case recorded
        malpracticeCaseRepository.save(new MalpracticeCase(
                exam, students.get(18), c1, chief.getEmail(),
                "Candidate found in possession of unauthorized handwritten notes during examination."
        ));

        // FR11 Revaluation Request recorded
        revaluationRequestRepository.save(new RevaluationRequest(
                students.get(10), c2, exam,
                "Requesting valuation recheck for Question 14b (OOSE Architectural Design).",
                true, BigDecimal.valueOf(45.00)
        ));

        auditService.logAction("SEED_DEMO_DATA", "System", "0", null, "Initial demonstration database seeded successfully.");
    }

    private User createUser(String email, String rawPass, String fullName, Role role) {
        User u = new User(email, passwordEncoder.encode(rawPass), fullName, role);
        return userRepository.save(u);
    }

    private Faculty createFaculty(String email, String rawPass, String fullName, Department dept, String designation) {
        User u = createUser(email, rawPass, fullName, Role.FACULTY);
        Faculty f = new Faculty(u, fullName, dept, designation, email);
        return facultyRepository.save(f);
    }

    private String getStudentName(int idx) {
        String[] names = {
                "Aarav Sharma", "Aditi Patel", "Akash Verma", "Ananya Reddy",
                "Arjun Nair", "Bhavna Rao", "Chetan Kumar", "Deepika Iyer",
                "Divya Sundaram", "Gautam Menon", "Harish Pillai", "Ishaan Joshi",
                "Kavya Hegde", "Madhav Kulkarni", "Meera Krishnan", "Naveen Raj",
                "Neha Nambiar", "Pranav Deshmukh", "Pooja Swaminathan", "Rahul Sen",
                "Rohan Bhat", "Riya Mukherjee", "Sai Pradeep", "Sanjay Murthy",
                "Shruti Natarajan", "Siddharth Das", "Sneha Kothari", "Tarun Kapoor",
                "Varun Venkatesh", "Yashwant Prasad"
        };
        return names[(idx - 1) % names.length];
    }
}
