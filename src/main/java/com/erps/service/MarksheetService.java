package com.erps.service;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.ResultRepository;
import com.erps.repository.SemesterResultRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class MarksheetService {

    private final SemesterResultRepository semesterResultRepository;
    private final ResultRepository resultRepository;
    private final InstitutionService institutionService;
    private final AuditService auditService;

    @Value("${server.port:8080}")
    private String serverPort;

    public MarksheetService(SemesterResultRepository semesterResultRepository,
                            ResultRepository resultRepository,
                            InstitutionService institutionService,
                            AuditService auditService) {
        this.semesterResultRepository = semesterResultRepository;
        this.resultRepository = resultRepository;
        this.institutionService = institutionService;
        this.auditService = auditService;
    }

    /**
     * Public verification data transfer record. Leaks NO marks or numbers.
     */
    public record VerificationSummary(boolean valid, String studentInitials, String examName, String publishedDate, String resultReference) {}

    public VerificationSummary verifyResultReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return new VerificationSummary(false, null, null, null, null);
        }

        Optional<SemesterResult> semResultOpt = semesterResultRepository.findByResultReference(reference.trim());
        if (semResultOpt.isEmpty() || semResultOpt.get().getWorkflowStatus() != WorkflowStatus.PUBLISHED) {
            return new VerificationSummary(false, null, null, null, reference);
        }

        SemesterResult sr = semResultOpt.get();
        String initials = computeInitials(sr.getStudent().getName());
        String examName = sr.getExam().getName();
        String pubDate = sr.getPublishedAt() != null
                ? sr.getPublishedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                : "N/A";

        return new VerificationSummary(true, initials, examName, pubDate, sr.getResultReference());
    }

    private String computeInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "N.A.";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(". ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Generates a single semester PDF mark sheet with QR code, configurable institution header,
     * tabular results, GPA, and security disclaimer footer.
     */
    @Transactional
    public byte[] generateSemesterMarksheetPdf(Long semesterResultId, boolean isDuplicate) {
        SemesterResult sr = semesterResultRepository.findById(semesterResultId)
                .orElseThrow(() -> new IllegalArgumentException("Semester result record not found"));

        if (sr.getWorkflowStatus() != WorkflowStatus.PUBLISHED) {
            throw new IllegalStateException("Mark sheets can only be generated for published results.");
        }

        auditService.logAction(isDuplicate ? "DOWNLOAD_DUPLICATE_MARKSHEET" : "DOWNLOAD_MARKSHEET",
                "SemesterResult", sr.getId().toString(), null,
                "Ref: " + sr.getResultReference() + ", Student: " + sr.getStudent().getRegisterNo());

        InstitutionSettings inst = institutionService.getSettings();
        List<Result> courseResults = resultRepository.findByExamIdAndStudentId(sr.getExam().getId(), sr.getStudent().getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Header Font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Font.BOLD);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Font.ITALIC);

            // Institution Title
            Paragraph instTitle = new Paragraph(inst.getName().toUpperCase(), titleFont);
            instTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(instTitle);

            Paragraph instSub = new Paragraph(inst.getAddress() + "\n(Affiliated to " + inst.getUniversityName() + ")", subTitleFont);
            instSub.setAlignment(Element.ALIGN_CENTER);
            instSub.setSpacingAfter(10);
            document.add(instSub);

            // Title banner
            String bannerText = isDuplicate ? "GRADE SHEET / STATEMENT OF MARKS (DUPLICATE)" : "GRADE SHEET / STATEMENT OF MARKS";
            Paragraph banner = new Paragraph(bannerText, boldFont);
            banner.setAlignment(Element.ALIGN_CENTER);
            banner.setSpacingAfter(15);
            document.add(banner);

            // Student Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{20, 30, 20, 30});

            addInfoCell(infoTable, "Register No:", sr.getStudent().getRegisterNo(), boldFont, regularFont);
            addInfoCell(infoTable, "Student Name:", sr.getStudent().getName(), boldFont, regularFont);
            addInfoCell(infoTable, "Programme:", sr.getStudent().getProgramme().getName(), boldFont, regularFont);
            addInfoCell(infoTable, "Department:", sr.getStudent().getDepartment().getName(), boldFont, regularFont);
            addInfoCell(infoTable, "Regulation:", sr.getStudent().getRegulation().getCode(), boldFont, regularFont);
            addInfoCell(infoTable, "Semester / Year:", "Semester " + sr.getExam().getSemester(), boldFont, regularFont);
            addInfoCell(infoTable, "Examination:", sr.getExam().getName(), boldFont, regularFont);
            addInfoCell(infoTable, "Academic Year:", sr.getExam().getAcademicYear(), boldFont, regularFont);

            infoTable.setSpacingAfter(15);
            document.add(infoTable);

            // Results Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{15, 45, 10, 10, 10, 10});

            addHeaderCell(table, "Course Code", boldFont);
            addHeaderCell(table, "Course Title", boldFont);
            addHeaderCell(table, "Credits", boldFont);
            addHeaderCell(table, "Grade", boldFont);
            addHeaderCell(table, "Grade Pt", boldFont);
            addHeaderCell(table, "Result", boldFont);

            for (Result r : courseResults) {
                addBodyCell(table, r.getCourse().getCode(), regularFont);
                addBodyCell(table, r.getCourse().getTitle(), regularFont);
                addBodyCell(table, r.getCourse().getCredits() != null ? r.getCourse().getCredits().toPlainString() : "0.0", regularFont);
                addBodyCell(table, r.getGrade() != null ? r.getGrade() : "N/A", regularFont);
                addBodyCell(table, r.getGradePoint() != null ? r.getGradePoint().toString() : "0", regularFont);
                addBodyCell(table, r.getStatus().name(), regularFont);
            }
            table.setSpacingAfter(15);
            document.add(table);

            // Summary Table (GPA, CGPA, Result)
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{25, 25, 25, 25});

            addSummaryCell(summaryTable, "Semester GPA", sr.getGpa() != null ? sr.getGpa().toPlainString() : "0.00", boldFont);
            addSummaryCell(summaryTable, "Cumulative CGPA", sr.getCgpa() != null ? sr.getCgpa().toPlainString() : "0.00", boldFont);
            addSummaryCell(summaryTable, "Arrears", String.valueOf(sr.getArrearCount()), boldFont);
            addSummaryCell(summaryTable, "Final Outcome", sr.isPass() ? "PASSED" : "ARREARS PENDING", boldFont);

            summaryTable.setSpacingAfter(20);
            document.add(summaryTable);

            // Footer Section: QR Code & Verification link
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{75, 25});

            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.addElement(new Paragraph("Result Reference: " + sr.getResultReference(), boldFont));
            String pubDateStr = sr.getPublishedAt() != null ? sr.getPublishedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "N/A";
            textCell.addElement(new Paragraph("Publication Date: " + pubDateStr, regularFont));
            textCell.addElement(new Paragraph("\nController of Examinations                                               Principal / Chairman RPB", regularFont));
            footerTable.addCell(textCell);

            // QR Code Cell
            String qrUrl = "http://localhost:" + serverPort + "/verify?ref=" + sr.getResultReference();
            byte[] qrImageBytes = generateQrCodePng(qrUrl, 100, 100);
            Image qrImage = Image.getInstance(qrImageBytes);
            qrImage.scaleToFit(80, 80);

            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            qrCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(qrCell);

            document.add(footerTable);

            // Mandatory DEMO Document Disclaimer Footer
            Paragraph disclaimer = new Paragraph("\n\n* DEMO DOCUMENT, NOT AN OFFICIAL CERTIFICATE. Generated for institutional demonstration only.", footerFont);
            disclaimer.setAlignment(Element.ALIGN_CENTER);
            document.add(disclaimer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF marksheet", e);
        }
    }

    private byte[] generateQrCodePng(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "N/A", valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addSummaryCell(PdfPTable table, String title, String val, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(new Paragraph(title, font));
        cell.addElement(new Paragraph(val, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
