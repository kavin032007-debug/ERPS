package com.erps.view;

import com.erps.entity.*;
import com.erps.service.ExaminationService;
import com.erps.service.ResultService;
import com.erps.service.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;

@Route(value = "results", layout = MainLayout.class)
@PageTitle("Result Calculation Engine | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class ResultCalculationView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final ResultService resultService;
    private final WorkflowService workflowService;

    private final Select<Examination> examSelect = new Select<>();
    private final Grid<SemesterResult> semResultGrid = new Grid<>(SemesterResult.class, false);

    public ResultCalculationView(ExaminationService examinationService,
                                 ResultService resultService,
                                 WorkflowService workflowService) {
        this.examinationService = examinationService;
        this.resultService = resultService;
        this.workflowService = workflowService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("Automated Result Calculation & Policy Engine (FR6)");

        HorizontalLayout bar = new HorizontalLayout();
        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("380px");

        examSelect.addValueChangeListener(e -> refreshResults());

        Button calculateBtn = new Button("Run Calculation Engine", VaadinIcon.COG.create(), e -> executeCalculation());
        calculateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button submitWorkflowBtn = new Button("Submit for Verification (DRAFT -> SUBMITTED)", VaadinIcon.ARROW_RIGHT.create(), e -> submitWorkflow());
        submitWorkflowBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        bar.add(examSelect, calculateBtn, submitWorkflowBtn);

        configureGrid();

        refreshResults();
        add(title, bar, semResultGrid);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void configureGrid() {
        semResultGrid.setSizeFull();
        semResultGrid.addColumn(sr -> sr.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px").setSortable(true);
        semResultGrid.addColumn(sr -> sr.getStudent().getName()).setHeader("Student Name").setFlexGrow(1).setSortable(true);
        semResultGrid.addColumn(sr -> sr.getGpa() != null ? sr.getGpa().toPlainString() : "0.00").setHeader("GPA").setWidth("100px").setSortable(true);
        semResultGrid.addColumn(sr -> sr.getCgpa() != null ? sr.getCgpa().toPlainString() : "0.00").setHeader("CGPA").setWidth("100px").setSortable(true);
        semResultGrid.addColumn(SemesterResult::getArrearCount).setHeader("Arrears").setWidth("100px");
        semResultGrid.addColumn(sr -> sr.getRankPosition() != null ? "#" + sr.getRankPosition() : "-").setHeader("Rank").setWidth("100px").setSortable(true);

        semResultGrid.addComponentColumn(sr -> {
            Span badge = new Span(sr.isPass() ? "PASSED" : "ARREARS");
            badge.getElement().getThemeList().add(sr.isPass() ? "badge success small" : "badge error small");
            return badge;
        }).setHeader("Outcome").setWidth("120px");

        semResultGrid.addComponentColumn(sr -> {
            Span stageBadge = new Span(sr.getWorkflowStatus().name());
            stageBadge.getElement().getThemeList().add("badge primary small");
            return stageBadge;
        }).setHeader("Workflow Stage").setWidth("150px");

        semResultGrid.addComponentColumn(sr -> {
            Button inspectBtn = new Button("How calculated?", VaadinIcon.INFO_CIRCLE.create(), ev -> openExplanationDialog(sr));
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return inspectBtn;
        }).setHeader("Formula Explanation").setWidth("180px");
    }

    private void executeCalculation() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        try {
            Map<String, Object> summary = resultService.calculateResultsForExam(exam.getId());
            Notification.show(String.format("Calculation complete! Students: %s, Incomplete: %s, Held: %s",
                    summary.get("totalStudents"), summary.get("incompleteCount"), summary.get("heldCount")),
                    4000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshResults();
        } catch (Exception ex) {
            Notification.show("Calculation Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void submitWorkflow() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        try {
            workflowService.submitResults(exam.getId(), "Controller of Examinations");
            Notification.show("Results calculated and submitted for CoE verification.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshResults();
        } catch (Exception ex) {
            Notification.show("Workflow Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openExplanationDialog(SemesterResult sr) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Calculation Breakdown: " + sr.getStudent().getRegisterNo() + " (" + sr.getStudent().getName() + ")");
        dialog.setWidth("750px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        List<Result> courses = resultService.getCourseResults(sr.getExam().getId(), sr.getStudent().getId());
        for (Result r : courses) {
            VerticalLayout cBox = new VerticalLayout();
            cBox.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-15pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "10px")
                    .set("background", "var(--lumo-contrast-5pct)");

            H4 cTitle = new H4(r.getCourse().getCode() + ": " + r.getCourse().getTitle() + " (" + r.getCourse().getCredits() + " credits)");
            cTitle.getStyle().set("margin", "0 0 5px 0");

            Paragraph exp = new Paragraph(r.getExplanation());
            exp.getStyle().set("margin", "0").set("font-family", "monospace").set("font-size", "var(--lumo-font-size-xs)");

            cBox.add(cTitle, exp);
            layout.add(cBox);
        }

        Paragraph gpaFormula = new Paragraph("GPA Formula: sum(credit x grade_point) / sum(credits) = " + (sr.getGpa() != null ? sr.getGpa().toPlainString() : "0.00"));
        gpaFormula.getStyle().set("font-weight", "600");
        layout.add(gpaFormula);

        dialog.add(layout);
        Button close = new Button("Close", ev -> dialog.close());
        dialog.getFooter().add(close);
        dialog.open();
    }

    private void refreshResults() {
        Examination exam = examSelect.getValue();
        if (exam != null) {
            semResultGrid.setItems(resultService.getSemesterResultsForExam(exam.getId()));
        } else {
            semResultGrid.setItems(List.of());
        }
    }
}
