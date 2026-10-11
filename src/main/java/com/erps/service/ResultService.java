package com.erps.service;

import com.erps.dto.CalculationDetails;
import com.erps.dto.CourseResultItem;
import com.erps.dto.StudentRankEntry;
import com.erps.engine.GradingEngine;
import com.erps.entity.*;
import com.erps.ranking.RankingService;
import com.erps.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResultService {

    private final ExaminationRepository examinationRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final ExamCourseRepository examCourseRepository;
    private final MarksRepository marksRepository;
    private final ResultRepository resultRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final GradeScaleRepository gradeScaleRepository;
    private final GradingConfigRepository gradingConfigRepository;
    private final GradingEngine gradingEngine;
    private final RankingService rankingService;
    private final SecureRandom random = new SecureRandom();

    public ResultService(ExaminationRepository examinationRepository,
                         CourseRegistrationRepository courseRegistrationRepository,
                         ExamCourseRepository examCourseRepository,
                         MarksRepository marksRepository,
                         ResultRepository resultRepository,
                         SemesterResultRepository semesterResultRepository,
                         GradeScaleRepository gradeScaleRepository,
                         GradingConfigRepository gradingConfigRepository,
                         GradingEngine gradingEngine,
                         RankingService rankingService) {
        this.examinationRepository = examinationRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.examCourseRepository = examCourseRepository;
        this.marksRepository = marksRepository;
        this.resultRepository = resultRepository;
        this.semesterResultRepository = semesterResultRepository;
        this.gradeScaleRepository = gradeScaleRepository;
        this.gradingConfigRepository = gradingConfigRepository;
        this.gradingEngine = gradingEngine;
        this.rankingService = rankingService;
    }

    /**
     * Executes transparent calculation of all student results for an examination in a single transaction.
     * Returns a summary report of the calculation.
     */
    @Transactional
    public Map<String, Object> calculateResultsForExam(Long examId) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Examination not found"));

        Regulation regulation = exam.getRegulation();
        List<GradeScale> gradeScales = regulation != null
                ? gradeScaleRepository.findByRegulationIdOrderByMinMarksDesc(regulation.getId())
                : Collections.emptyList();
        GradingConfig config = regulation != null
                ? gradingConfigRepository.findByRegulationId(regulation.getId()).orElse(null)
                : null;

        List<CourseRegistration> registrations = courseRegistrationRepository.findByExamId(examId);
        if (registrations.isEmpty()) {
            throw new IllegalStateException("No student registrations found for this examination.");
        }

        // Group registrations by student
        Map<Student, List<CourseRegistration>> studentRegs = new LinkedHashMap<>();
        for (CourseRegistration reg : registrations) {
            studentRegs.computeIfAbsent(reg.getStudent(), k -> new ArrayList<>()).add(reg);
        }

        // Pre-fetch ExamCourses
        List<ExamCourse> examCourses = examCourseRepository.findByExamId(examId);
        Map<Long, ExamCourse> courseToExamCourse = new HashMap<>();
        for (ExamCourse ec : examCourses) {
            courseToExamCourse.put(ec.getCourse().getId(), ec);
        }

        List<StudentRankEntry> rankEntries = new ArrayList<>();
        Map<Long, SemesterResult> studentSemesterResults = new HashMap<>();
        int incompleteCount = 0;
        int heldCount = 0;

        for (Map.Entry<Student, List<CourseRegistration>> entry : studentRegs.entrySet()) {
            Student student = entry.getKey();
            List<CourseRegistration> regs = entry.getValue();

            List<CourseResultItem> studentCourseItems = new ArrayList<>();
            BigDecimal studentTotalMarks = BigDecimal.ZERO;
            int arrearCount = 0;
            BigDecimal totalCredits = BigDecimal.ZERO;
            BigDecimal earnedCredits = BigDecimal.ZERO;
            boolean hasIncomplete = false;
            boolean hasHeld = false;

            for (CourseRegistration reg : regs) {
                Course course = reg.getCourse();
                ExamCourse ec = courseToExamCourse.get(course.getId());
                Marks marks = null;
                if (ec != null) {
                    marks = marksRepository.findByExamCourseIdAndStudentId(ec.getId(), student.getId()).orElse(null);
                }

                CalculationDetails calc = gradingEngine.computeCourseResult(course, marks, gradeScales, config);

                if (calc.getStatus() == ResultStatus.INCOMPLETE) {
                    hasIncomplete = true;
                    incompleteCount++;
                }
                if (calc.getStatus() == ResultStatus.HELD) {
                    hasHeld = true;
                    heldCount++;
                }
                if (calc.getStatus() == ResultStatus.FAIL || calc.getStatus() == ResultStatus.ABSENT || calc.getStatus() == ResultStatus.HELD) {
                    arrearCount++;
                }

                // Save or update Course Result
                Optional<Result> existingResultOpt = resultRepository.findByExamIdAndStudentIdAndCourseId(
                        exam.getId(), student.getId(), course.getId());
                Result result = existingResultOpt.orElseGet(Result::new);
                result.setExam(exam);
                result.setStudent(student);
                result.setCourse(course);
                result.setCiaMarks(calc.getCiaScaled());
                result.setSeeMarks(calc.getSeeScaled());
                result.setTotal(calc.getTotalMarks());
                result.setGrade(calc.getGrade());
                result.setGradePoint(calc.getGradePoint());
                result.setStatus(calc.getStatus());
                result.setExplanation(calc.getExplanation());
                resultRepository.save(result);

                // For GPA calculation
                studentCourseItems.add(new CourseResultItem(
                        course.getCode(), course.getTitle(), course.getCredits(),
                        calc.getGrade(), calc.getGradePoint(), calc.getStatus(), calc.getTotalMarks()
                ));

                if (course.getCredits() != null) {
                    totalCredits = totalCredits.add(course.getCredits());
                    if (calc.getStatus() == ResultStatus.PASS) {
                        earnedCredits = earnedCredits.add(course.getCredits());
                    }
                }
                studentTotalMarks = studentTotalMarks.add(calc.getTotalMarks());
            }

            BigDecimal gpa = gradingEngine.computeGpa(studentCourseItems);

            // Fetch previous results for CGPA across all exams
            List<Result> allStudentPastResults = resultRepository.findByStudentId(student.getId());
            List<CourseResultItem> allAttemptItems = new ArrayList<>();
            for (Result r : allStudentPastResults) {
                allAttemptItems.add(new CourseResultItem(
                        r.getCourse().getCode(), r.getCourse().getTitle(), r.getCourse().getCredits(),
                        r.getGrade(), r.getGradePoint(), r.getStatus(), r.getTotal()
                ));
            }
            BigDecimal cgpa = gradingEngine.computeCgpa(allAttemptItems);

            // Existing SemesterResult or new
            Optional<SemesterResult> existingSemOpt = semesterResultRepository.findByExamIdAndStudentId(exam.getId(), student.getId());
            SemesterResult semResult = existingSemOpt.orElseGet(SemesterResult::new);
            semResult.setExam(exam);
            semResult.setStudent(student);
            semResult.setGpa(gpa);
            semResult.setCgpa(cgpa);
            semResult.setTotalCredits(totalCredits);
            semResult.setEarnedCredits(earnedCredits);
            semResult.setArrearCount(arrearCount);

            if (semResult.getResultReference() == null) {
                semResult.setResultReference(generateResultReference(exam.getId(), student.getRegisterNo()));
            }

            studentSemesterResults.put(student.getId(), semResult);
            rankEntries.add(new StudentRankEntry(student.getId(), student.getRegisterNo(), student.getName(),
                    gpa, studentTotalMarks, arrearCount));
        }

        // Compute rankings
        boolean rankRequiresNoArrear = config == null || Boolean.TRUE.equals(config.getRankRequiresNoArrear());
        List<StudentRankEntry> ranked = rankingService.computeRanks(rankEntries, rankRequiresNoArrear);
        for (StudentRankEntry r : ranked) {
            SemesterResult semRes = studentSemesterResults.get(r.getStudentId());
            if (semRes != null) {
                semRes.setRankPosition(r.getRank());
                semesterResultRepository.save(semRes);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalStudents", studentRegs.size());
        summary.put("incompleteCount", incompleteCount);
        summary.put("heldCount", heldCount);
        summary.put("status", "SUCCESS");
        return summary;
    }

    private String generateResultReference(Long examId, String regNo) {
        int token = 1000 + random.nextInt(9000);
        return String.format("ERPS-%d-%s-%04d", examId, regNo, token);
    }

    public List<SemesterResult> getSemesterResultsForExam(Long examId) {
        return semesterResultRepository.findByExamId(examId);
    }

    public Optional<SemesterResult> getSemesterResultForStudent(Long examId, Long studentId) {
        return semesterResultRepository.findByExamIdAndStudentId(examId, studentId);
    }

    public List<Result> getCourseResults(Long examId, Long studentId) {
        return resultRepository.findByExamIdAndStudentId(examId, studentId);
    }
}
