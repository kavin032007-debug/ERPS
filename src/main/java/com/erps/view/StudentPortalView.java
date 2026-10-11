package com.erps.view;

import com.erps.entity.*;
import com.erps.repository.ResultRepository;
import com.erps.repository.SemesterResultRepository;
import com.erps.repository.StudentRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.MarksheetService;
import com.erps.service.RevaluationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

@Route(value = "student-portal", layout = MainLayout.class)
@PageTitle("Student Published Results & Mark Sheets | ERPS")
@RolesAllowed({"STUDENT", "SUPER_ADMIN"})
public class StudentPortalView extends VerticalLayout {

    private final StudentRepository studentRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final ResultRepository resultRepository;
    private final MarksheetService marksheetService;
    private final RevaluationService revaluationService;
    private final AuthenticatedUser authenticatedUser;

    private Student currentStudent;
    private final Select<SemesterResult> semSelect = new Select<>();
    private final Grid<Result> courseGrid = new Grid<>(Result.class, false);
    private final HorizontalLayout cardsLayout = new HorizontalLayout();

    public StudentPortalView(StudentRepository studentRepository,
                             SemesterResultRepository semesterResultRepository,
                             ResultRepository resultRepository,
                             MarksheetService marksheetService,
                             RevaluationService revaluationService,
                             AuthenticatedUser authenticatedUser) {
        this.studentRepository = studentRepository;
        this.semesterResultRepository = semesterResultRepository;
        this.resultRepository = resultRepository;
        this.marksheetService = marksheetService;
        this.revaluationService = revaluationService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        User user = authenticatedUser.get().orElse(null);
        if (user == null) {
            add(new Paragraph("Access denied."));
            return;
        }

        if (user.getRole() == Role.STUDENT) {
            currentStudent = studentRepository.findByUserId(user.getId()).orElse(null);
        } else {
            // Admin testing view: default to first student
            List<Student> all = studentRepository.findAll();
            if (!all.isEmpty()) currentStudent = all.get(0);
        }

        if (currentStudent == null) {
            add(new Paragraph("No student profile associated with this account."));
            return;
        }

        H2 title = new H2("Academic Performance & Official Mark Sheets");
        Paragraph subtitle = new Paragraph("Student: " + currentStudent.getName() + " (" + currentStudent.getRegisterNo() + ") | Programme: " + currentStudent.getProgramme().getName());
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Top KPI Cards
        cardsLayout.setWidthFull();
        updateKpiCards();

        // Semester Selection Bar & Downloads
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        semSelect.setLabel("Select Published Examination");
        semSelect.setItemLabelGenerator(sr -> sr.getExam().getName() + " (Sem " + sr.getExam().getSemester() + ")");
        List<SemesterResult> publishedResults = semesterResultRepository.findByStudentIdAndWorkflowStatus(currentStudent.getId(), WorkflowStatus.PUBLISHED);
        semSelect.setItems(publishedResults);
        semSelect.setWidth("380px");

        semSelect.addValueChangeListener(e -> refreshCourseGrid());

        // PDF Download Anchor
        Anchor downloadPdfBtn = new Anchor();
        downloadPdfBtn.getElement().setAttribute("download", true);
        Button pdfBtn = new Button("Download Mark Sheet (PDF)", VaadinIcon.DOWNLOAD.create());
        pdfBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        downloadPdfBtn.add(pdfBtn);

        // Duplicate PDF Anchor
        Anchor downloadDupBtn = new Anchor();
        downloadDupBtn.getElement().setAttribute("download", true);
        Button dupBtn = new Button("Request Duplicate Copy", VaadinIcon.COPY.create());
        dupBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        downloadDupBtn.add(dupBtn);

        Button revalBtn = new Button("Raise Revaluation Request", VaadinIcon.REFRESH.create(), e -> openRevalDialog());
        revalBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        bar.add(semSelect, downloadPdfBtn, downloadDupBtn, revalBtn);

        configureCourseGrid();

        add(title, subtitle, cardsLayout, bar, courseGrid);

        if (!publishedResults.isEmpty()) {
            SemesterResult latest = publishedResults.get(publishedResults.size() - 1);
            semSelect.setValue(latest);
            setupDownloadAnchors(downloadPdfBtn, downloadDupBtn, latest);
        }

        semSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                setupDownloadAnchors(downloadPdfBtn, downloadDupBtn, e.getValue());
            }
        });
    }

    private void setupDownloadAnchors(Anchor regular, Anchor duplicate, SemesterResult sr) {
        regular.setHref(new StreamResource("Marksheet_" + currentStudent.getRegisterNo() + ".pdf",
                () -> new ByteArrayInputStream(marksheetService.generateSemesterMarksheetPdf(sr.getId(), false))));

        duplicate.setHref(new StreamResource("Marksheet_DUPLICATE_" + currentStudent.getRegisterNo() + ".pdf",
                () -> new ByteArrayInputStream(marksheetService.generateSemesterMarksheetPdf(sr.getId(), true))));
    }

    private void configureCourseGrid() {
        courseGrid.setSizeFull();
        courseGrid.addColumn(r -> r.getCourse().getCode()).setHeader("Course Code").setWidth("140px");
        courseGrid.addColumn(r -> r.getCourse().getTitle()).setHeader("Course Title").setFlexGrow(2);
        courseGrid.addColumn(r -> r.getCourse().getCredits() + " cr").setHeader("Credits").setWidth("100px");
        courseGrid.addColumn(Result::getGrade).setHeader("Letter Grade").setWidth("130px").setSortable(true);
        courseGrid.addColumn(Result::getGradePoint).setHeader("Grade Point").setWidth("120px");

        courseGrid.addComponentColumn(r -> {
            Span b = new Span(r.getStatus() == ResultStatus.HELD ? "Result withheld" : r.getStatus().name());
            String theme = switch (r.getStatus()) {
                case PASS -> "badge success small";
                case FAIL, ABSENT -> "badge error small";
                case HELD -> "badge warning small";
                default -> "badge contrast small";
            };
            b.getElement().getThemeList().add(theme);
            return b;
        }).setHeader("Result Status").setWidth("150px");
    }

    private void refreshCourseGrid() {
        SemesterResult sr = semSelect.getValue();
        if (sr != null) {
            courseGrid.setItems(resultRepository.findByExamIdAndStudentId(sr.getExam().getId(), currentStudent.getId()));
        } else {
            courseGrid.setItems(List.of());
        }
    }

    private void updateKpiCards() {
        cardsLayout.removeAll();
        List<SemesterResult> publishedResults = semesterResultRepository.findByStudentIdAndWorkflowStatus(currentStudent.getId(), WorkflowStatus.PUBLISHED);

        if (publishedResults.isEmpty()) {
            cardsLayout.add(new Paragraph("No published examination results available yet."));
            return;
        }

        SemesterResult latest = publishedResults.get(publishedResults.size() - 1);

        cardsLayout.add(createCard("Semester GPA", latest.getGpa() != null ? latest.getGpa().toPlainString() : "0.00", VaadinIcon.TROPHY, "primary"));
        cardsLayout.add(createCard("Cumulative CGPA", latest.getCgpa() != null ? latest.getCgpa().toPlainString() : "0.00", VaadinIcon.STAR, "success"));
        cardsLayout.add(createCard("Pending Arrears", String.valueOf(latest.getArrearCount()), VaadinIcon.WARNING, latest.isPass() ? "success" : "error"));
        cardsLayout.add(createCard("Class Rank", latest.getRankPosition() != null ? "#" + latest.getRankPosition() : "Unranked", VaadinIcon.MEDAL, "contrast"));
    }

    private void openRevalDialog() {
        SemesterResult sr = semSelect.getValue();
        if (sr == null) {
            Notification.show("Please select a published examination first.");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Apply for Examination Revaluation");
        dialog.setWidth("550px");

        List<Result> courses = resultRepository.findByExamIdAndStudentId(sr.getExam().getId(), currentStudent.getId());
        Select<Result> courseSelect = new Select<>();
        courseSelect.setLabel("Select Course to Revalue");
        courseSelect.setItemLabelGenerator(r -> r.getCourse().getCode() + " - " + r.getCourse().getTitle() + " (Grade: " + r.getGrade() + ")");
        courseSelect.setItems(courses);

        TextArea reasonArea = new TextArea("Justification / Specific Questions to Re-evaluate");
        reasonArea.setWidthFull();
        reasonArea.setPlaceholder("State explicit question numbers and justification for revaluation.");

        Checkbox photocopyCheck = new Checkbox("Request Answer Script Photocopy first (Policy Workflow)");
        photocopyCheck.setValue(true);

        dialog.add(new FormLayout(courseSelect, reasonArea, photocopyCheck));

        Button submitBtn = new Button("Submit Revaluation Request", e -> {
            Result chosen = courseSelect.getValue();
            if (chosen == null) return;
            try {
                revaluationService.submitRequest(currentStudent, chosen.getCourse(), sr.getExam(),
                        reasonArea.getValue(), photocopyCheck.getValue(), chosen.getSeeMarks());
                Notification.show("Revaluation request submitted successfully. Forwarded to Examination Desk.",
                        4000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, submitBtn);
        dialog.open();
    }

    private VerticalLayout createCard(String title, String value, VaadinIcon icon, String theme) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        HorizontalLayout top = new HorizontalLayout(new Span(title), icon.create());
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H2 val = new H2(value);
        val.getStyle().set("margin", "6px 0 0 0");

        card.add(top, val);
        return card;
    }
}
