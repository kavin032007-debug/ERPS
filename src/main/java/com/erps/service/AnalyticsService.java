package com.erps.service;

import com.erps.entity.*;
import com.erps.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

@Service
public class AnalyticsService {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ExaminationRepository examinationRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final ResultRepository resultRepository;
    private final CourseRepository courseRepository;
    private final InstitutionService institutionService;

    public AnalyticsService(StudentRepository studentRepository,
                            FacultyRepository facultyRepository,
                            ExaminationRepository examinationRepository,
                            SemesterResultRepository semesterResultRepository,
                            ResultRepository resultRepository,
                            CourseRepository courseRepository,
                            InstitutionService institutionService) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.examinationRepository = examinationRepository;
        this.semesterResultRepository = semesterResultRepository;
        this.resultRepository = resultRepository;
        this.courseRepository = courseRepository;
        this.institutionService = institutionService;
    }

    public record DashboardOverview(
            long totalStudents,
            long totalFaculty,
            long totalExaminations,
            long totalPublished,
            long passedCount,
            long failedCount,
            double passPercentage
    ) {}

    public DashboardOverview getDashboardOverview(Long examId) {
        long students = studentRepository.count();
        long faculty = facultyRepository.count();
        long exams = examinationRepository.count();

        List<SemesterResult> semResults = examId != null
                ? semesterResultRepository.findByExamIdAndWorkflowStatus(examId, WorkflowStatus.PUBLISHED)
                : semesterResultRepository.findAll().stream().filter(r -> r.getWorkflowStatus() == WorkflowStatus.PUBLISHED).toList();

        long totalPublished = semResults.size();
        long passed = semResults.stream().filter(SemesterResult::isPass).count();
        long failed = totalPublished - passed;
        double passPct = totalPublished > 0 ? ((double) passed / totalPublished) * 100.0 : 0.0;

        return new DashboardOverview(students, faculty, exams, totalPublished, passed, failed, Math.round(passPct * 10.0) / 10.0);
    }

    public record CourseAnalysisRow(
            String courseCode,
            String courseTitle,
            int appeared,
            int passed,
            int failed,
            double passPercentage,
            BigDecimal highestMark,
            BigDecimal lowestMark,
            BigDecimal averageMark
    ) {}

    public List<CourseAnalysisRow> getCourseAnalysisForExam(Long examId) {
        List<Result> results = resultRepository.findByExamId(examId);
        Map<Course, List<Result>> byCourse = new HashMap<>();
        for (Result r : results) {
            byCourse.computeIfAbsent(r.getCourse(), k -> new ArrayList<>()).add(r);
        }

        List<CourseAnalysisRow> rows = new ArrayList<>();
        for (Map.Entry<Course, List<Result>> entry : byCourse.entrySet()) {
            Course c = entry.getKey();
            List<Result> resList = entry.getValue();

            int appeared = resList.size();
            int passed = (int) resList.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
            int failed = appeared - passed;
            double pct = appeared > 0 ? ((double) passed / appeared) * 100.0 : 0.0;

            BigDecimal max = resList.stream()
                    .map(Result::getTotal)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal min = resList.stream()
                    .map(Result::getTotal)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal sum = resList.stream()
                    .map(Result::getTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avg = appeared > 0 ? sum.divide(BigDecimal.valueOf(appeared), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            rows.add(new CourseAnalysisRow(
                    c.getCode(), c.getTitle(), appeared, passed, failed,
                    Math.round(pct * 10.0) / 10.0, max, min, avg
            ));
        }

        rows.sort(Comparator.comparing(CourseAnalysisRow::courseCode));
        return rows;
    }

    /**
     * Renders grade distribution chart purely in Java using JFreeChart, exported as base64 PNG.
     */
    public String generateGradeDistributionPngDataUri(Long examId, int width, int height) {
        List<Result> results = examId != null ? resultRepository.findByExamId(examId) : resultRepository.findAll();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String g : List.of("S", "A+", "A", "B+", "B", "C+", "C", "U", "AB", "WH")) {
            counts.put(g, 0);
        }

        for (Result r : results) {
            String g = r.getGrade() != null ? r.getGrade() : "U";
            counts.put(g, counts.getOrDefault(g, 0) + 1);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        counts.forEach((grade, count) -> dataset.addValue(count, "Students", grade));

        JFreeChart chart = ChartFactory.createBarChart(
                "Grade Distribution", "Letter Grade", "Number of Students",
                dataset, PlotOrientation.VERTICAL, false, true, false
        );
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(baos, chart, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate chart", e);
        }
    }

    /**
     * Renders Pass vs Fail distribution pie chart.
     */
    public String generatePassFailPieDataUri(Long examId, int width, int height) {
        DashboardOverview ov = getDashboardOverview(examId);
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataset.setValue("Passed (" + ov.passedCount() + ")", ov.passedCount());
        dataset.setValue("Failed (" + ov.failedCount() + ")", ov.failedCount());

        JFreeChart chart = ChartFactory.createPieChart(
                "Pass vs Fail Ratio", dataset, true, true, false
        );
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(baos, chart, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pie chart", e);
        }
    }

    /**
     * Exports Result Analysis Report for RPB as CSV.
     */
    public String exportRpbReportCsv(Long examId) {
        List<CourseAnalysisRow> rows = getCourseAnalysisForExam(examId);
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder()
                .setHeader("Course Code", "Course Title", "Appeared", "Passed", "Failed", "Pass %", "Highest", "Lowest", "Average")
                .build())) {
            for (CourseAnalysisRow r : rows) {
                printer.printRecord(r.courseCode(), r.courseTitle(), r.appeared(), r.passed(),
                        r.failed(), r.passPercentage(), r.highestMark(), r.lowestMark(), r.averageMark());
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV export failed", e);
        }
        return sw.toString();
    }

    /**
     * Exports Result Analysis Report for RPB as PDF.
     */
    public byte[] exportRpbReportPdf(Long examId) {
        List<CourseAnalysisRow> rows = getCourseAnalysisForExam(examId);
        InstitutionSettings inst = institutionService.getSettings();
        Examination exam = examinationRepository.findById(examId).orElse(null);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            com.lowagie.text.Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph title = new Paragraph(inst.getName().toUpperCase() + "\nRESULT PASSING BOARD - RESULT ANALYSIS REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            if (exam != null) {
                Paragraph sub = new Paragraph("Examination: " + exam.getName() + " | Academic Year: " + exam.getAcademicYear() + " | Semester: " + exam.getSemester(), boldFont);
                sub.setAlignment(Element.ALIGN_CENTER);
                sub.setSpacingAfter(15);
                document.add(sub);
            }

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{12, 32, 8, 8, 8, 8, 8, 8, 8});

            String[] headers = {"Course Code", "Course Title", "Appeared", "Passed", "Failed", "Pass %", "Highest", "Lowest", "Average"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setPadding(4);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (CourseAnalysisRow r : rows) {
                table.addCell(new Phrase(r.courseCode(), regularFont));
                table.addCell(new Phrase(r.courseTitle(), regularFont));
                table.addCell(new Phrase(String.valueOf(r.appeared()), regularFont));
                table.addCell(new Phrase(String.valueOf(r.passed()), regularFont));
                table.addCell(new Phrase(String.valueOf(r.failed()), regularFont));
                table.addCell(new Phrase(String.format("%.1f%%", r.passPercentage()), regularFont));
                table.addCell(new Phrase(r.highestMark().toPlainString(), regularFont));
                table.addCell(new Phrase(r.lowestMark().toPlainString(), regularFont));
                table.addCell(new Phrase(r.averageMark().toPlainString(), regularFont));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF export failed", e);
        }
    }
}
