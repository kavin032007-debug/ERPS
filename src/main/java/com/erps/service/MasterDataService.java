package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

@Service
public class MasterDataService {

    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final RegulationRepository regulationRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final UserRepository userRepository;
    private final GradeScaleRepository gradeScaleRepository;
    private final GradingConfigRepository gradingConfigRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public MasterDataService(DepartmentRepository departmentRepository,
                             ProgrammeRepository programmeRepository,
                             RegulationRepository regulationRepository,
                             SemesterRepository semesterRepository,
                             CourseRepository courseRepository,
                             StudentRepository studentRepository,
                             FacultyRepository facultyRepository,
                             FacultyAssignmentRepository facultyAssignmentRepository,
                             CourseRegistrationRepository courseRegistrationRepository,
                             UserRepository userRepository,
                             GradeScaleRepository gradeScaleRepository,
                             GradingConfigRepository gradingConfigRepository,
                             AuditService auditService,
                             PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.programmeRepository = programmeRepository;
        this.regulationRepository = regulationRepository;
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.userRepository = userRepository;
        this.gradeScaleRepository = gradeScaleRepository;
        this.gradingConfigRepository = gradingConfigRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    // Departments
    public List<Department> getAllDepartments() { return departmentRepository.findAll(); }
    public Optional<Department> getDepartmentById(Long id) { return departmentRepository.findById(id); }

    @Transactional
    public Department saveDepartment(Department dept) {
        boolean isNew = dept.getId() == null;
        Department saved = departmentRepository.save(dept);
        auditService.logAction(isNew ? "CREATE_DEPARTMENT" : "UPDATE_DEPARTMENT", "Department",
                saved.getId().toString(), null, saved.getCode() + " - " + saved.getName());
        return saved;
    }

    // Programmes
    public List<Programme> getAllProgrammes() { return programmeRepository.findAll(); }
    public List<Programme> getProgrammesByDepartment(Long departmentId) { return programmeRepository.findByDepartmentId(departmentId); }

    @Transactional
    public Programme saveProgramme(Programme prog) {
        boolean isNew = prog.getId() == null;
        Programme saved = programmeRepository.save(prog);
        auditService.logAction(isNew ? "CREATE_PROGRAMME" : "UPDATE_PROGRAMME", "Programme",
                saved.getId().toString(), null, saved.getCode() + " - " + saved.getName());
        return saved;
    }

    // Regulations
    public List<Regulation> getAllRegulations() { return regulationRepository.findAll(); }
    public Optional<Regulation> getRegulationById(Long id) { return regulationRepository.findById(id); }

    @Transactional
    public Regulation saveRegulation(Regulation reg) {
        boolean isNew = reg.getId() == null;
        Regulation saved = regulationRepository.save(reg);
        if (isNew) {
            // Seed default absolute grading scales for the regulation
            seedDefaultScalesForRegulation(saved);
        }
        auditService.logAction(isNew ? "CREATE_REGULATION" : "UPDATE_REGULATION", "Regulation",
                saved.getId().toString(), null, saved.getCode());
        return saved;
    }

    private void seedDefaultScalesForRegulation(Regulation reg) {
        gradingConfigRepository.save(new GradingConfig(reg));
        List<GradeScale> scales = List.of(
                new GradeScale(reg, "S", BigDecimal.valueOf(91.00), BigDecimal.valueOf(100.00), 10),
                new GradeScale(reg, "A+", BigDecimal.valueOf(81.00), BigDecimal.valueOf(90.99), 9),
                new GradeScale(reg, "A", BigDecimal.valueOf(71.00), BigDecimal.valueOf(80.99), 8),
                new GradeScale(reg, "B+", BigDecimal.valueOf(66.00), BigDecimal.valueOf(70.99), 7),
                new GradeScale(reg, "B", BigDecimal.valueOf(61.00), BigDecimal.valueOf(65.99), 6),
                new GradeScale(reg, "C+", BigDecimal.valueOf(56.00), BigDecimal.valueOf(60.99), 5),
                new GradeScale(reg, "C", BigDecimal.valueOf(50.00), BigDecimal.valueOf(55.99), 4),
                new GradeScale(reg, "U", BigDecimal.valueOf(0.00), BigDecimal.valueOf(49.99), 0)
        );
        gradeScaleRepository.saveAll(scales);
    }

    // Semesters
    public List<Semester> getAllSemesters() { return semesterRepository.findAll(); }
    @Transactional
    public Semester saveSemester(Semester sem) { return semesterRepository.save(sem); }

    // Courses
    public List<Course> getAllCourses() { return courseRepository.findAll(); }
    public Optional<Course> getCourseById(Long id) { return courseRepository.findById(id); }
    public Optional<Course> getCourseByCode(String code) { return courseRepository.findByCode(code); }

    @Transactional
    public Course saveCourse(Course course) {
        boolean isNew = course.getId() == null;
        Course saved = courseRepository.save(course);
        auditService.logAction(isNew ? "CREATE_COURSE" : "UPDATE_COURSE", "Course",
                saved.getId().toString(), null, saved.getCode() + " - " + saved.getTitle());
        return saved;
    }

    // Students
    public List<Student> getAllStudents() { return studentRepository.findAll(); }
    public Optional<Student> getStudentById(Long id) { return studentRepository.findById(id); }
    public Optional<Student> getStudentByRegisterNo(String regNo) { return studentRepository.findByRegisterNo(regNo); }
    public Optional<Student> getStudentByUserId(Long userId) { return studentRepository.findByUserId(userId); }

    @Transactional
    public Student saveStudent(Student student, String password) {
        boolean isNew = student.getId() == null;
        if (student.getUser() == null) {
            User user = new User(student.getEmail(), passwordEncoder.encode(password != null ? password : "Password@123"),
                    student.getName(), Role.STUDENT);
            user = userRepository.save(user);
            student.setUser(user);
        } else if (password != null && !password.isBlank()) {
            student.getUser().setPasswordHash(passwordEncoder.encode(password));
            student.getUser().setEmail(student.getEmail());
            student.getUser().setFullName(student.getName());
            userRepository.save(student.getUser());
        }

        Student saved = studentRepository.save(student);
        auditService.logAction(isNew ? "CREATE_STUDENT" : "UPDATE_STUDENT", "Student",
                saved.getId().toString(), null, saved.getRegisterNo() + " - " + saved.getName());
        return saved;
    }

    // Faculty & Faculty Assignments
    public List<Faculty> getAllFaculty() { return facultyRepository.findAll(); }
    public Optional<Faculty> getFacultyByUserId(Long userId) { return facultyRepository.findByUserId(userId); }

    @Transactional
    public Faculty saveFaculty(Faculty faculty, String password) {
        boolean isNew = faculty.getId() == null;
        if (faculty.getUser() == null) {
            User user = new User(faculty.getEmail(), passwordEncoder.encode(password != null ? password : "Password@123"),
                    faculty.getName(), Role.FACULTY);
            user = userRepository.save(user);
            faculty.setUser(user);
        } else if (password != null && !password.isBlank()) {
            faculty.getUser().setPasswordHash(passwordEncoder.encode(password));
            faculty.getUser().setEmail(faculty.getEmail());
            faculty.getUser().setFullName(faculty.getName());
            userRepository.save(faculty.getUser());
        }

        Faculty saved = facultyRepository.save(faculty);
        auditService.logAction(isNew ? "CREATE_FACULTY" : "UPDATE_FACULTY", "Faculty",
                saved.getId().toString(), null, saved.getName());
        return saved;
    }

    public List<FacultyAssignment> getAssignmentsForFaculty(Long facultyId) {
        return facultyAssignmentRepository.findByFacultyId(facultyId);
    }

    @Transactional
    public FacultyAssignment assignFacultyToCourse(Faculty faculty, Course course, String academicYear) {
        FacultyAssignment assignment = new FacultyAssignment(faculty, course, academicYear);
        FacultyAssignment saved = facultyAssignmentRepository.save(assignment);
        auditService.logAction("ASSIGN_FACULTY_COURSE", "FacultyAssignment", saved.getId().toString(),
                null, faculty.getName() + " -> " + course.getCode());
        return saved;
    }

    // Course Registrations
    public List<CourseRegistration> getRegistrationsForExam(Long examId) {
        return courseRegistrationRepository.findByExamId(examId);
    }

    public List<CourseRegistration> getRegistrationsForStudent(Long studentId) {
        return courseRegistrationRepository.findByStudentId(studentId);
    }

    @Transactional
    public CourseRegistration registerStudentCourse(Student student, Course course, Examination exam, String verifiedBy) {
        if (courseRegistrationRepository.existsByStudentIdAndCourseIdAndExamId(student.getId(), course.getId(), exam.getId())) {
            throw new IllegalArgumentException("Student is already registered for course " + course.getCode() + " in this examination.");
        }
        CourseRegistration reg = new CourseRegistration(student, course, exam, "REGISTERED", verifiedBy);
        CourseRegistration saved = courseRegistrationRepository.save(reg);
        auditService.logAction("COURSE_REGISTRATION", "CourseRegistration", saved.getId().toString(),
                null, student.getRegisterNo() + " -> " + course.getCode());
        return saved;
    }

    @Transactional
    public void verifyRegistration(Long registrationId, String coordinatorName) {
        CourseRegistration reg = courseRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        reg.setStatus("VERIFIED");
        reg.setVerifiedBy(coordinatorName);
        courseRegistrationRepository.save(reg);
        auditService.logAction("VERIFY_REGISTRATION", "CourseRegistration", registrationId.toString(),
                "REGISTERED", "VERIFIED by " + coordinatorName);
    }

    // CSV Bulk Import for Students (FR2)
    @Transactional
    public List<String> bulkImportStudentsCsv(String csvContent) {
        List<String> results = new ArrayList<>();
        try (Reader in = new StringReader(csvContent);
             CSVParser parser = new CSVParser(in, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    String regNo = record.get("register_no");
                    String name = record.get("name");
                    String email = record.get("email");
                    String deptCode = record.get("department_code");
                    String progCode = record.get("programme_code");
                    String regCode = record.get("regulation_code");
                    int batch = Integer.parseInt(record.get("batch_year"));
                    int currentSem = Integer.parseInt(record.get("current_semester"));

                    if (studentRepository.existsByRegisterNo(regNo)) {
                        results.add("Row " + rowNum + ": Skipped - Register number " + regNo + " already exists.");
                        continue;
                    }

                    Department dept = departmentRepository.findByCode(deptCode)
                            .orElseThrow(() -> new IllegalArgumentException("Department " + deptCode + " not found"));
                    Programme prog = programmeRepository.findByCode(progCode)
                            .orElseThrow(() -> new IllegalArgumentException("Programme " + progCode + " not found"));
                    Regulation reg = regulationRepository.findByCode(regCode)
                            .orElseThrow(() -> new IllegalArgumentException("Regulation " + regCode + " not found"));

                    Student student = new Student(null, regNo, name, dept, prog, reg, batch, currentSem, email);
                    saveStudent(student, "Password@123");
                    results.add("Row " + rowNum + ": Success - Imported student " + regNo + " (" + name + ")");
                } catch (Exception ex) {
                    results.add("Row " + rowNum + ": Error - " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            results.add("CSV Parsing Error: " + e.getMessage());
        }
        return results;
    }
}
