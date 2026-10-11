package com.erps.view;

import com.erps.entity.Examination;
import com.erps.service.AnalyticsService;
import com.erps.service.ExaminationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Analytics & Reports | ERPS")
@RolesAllowed({"SUPER_ADMIN", "PRINCIPAL", "TECH_STAFF", "EXAM_CELL_COORDINATOR", "FACULTY", "CHIEF_SUPERINTENDENT"})
public class AnalyticsView extends VerticalLayout {

    private final AnalyticsService analyticsService;
    private final ExaminationService examinationService;

    private final Select<Examination> examSelect = new Select<>();
    private final HorizontalLayout kpiRow = new HorizontalLayout();
    private final HorizontalLayout chartsRow = new HorizontalLayout();
    private final Grid<AnalyticsService.CourseAnalysisRow> courseGrid = new Grid<>();

    private final Anchor csvAnchor = new Anchor();
    private final Anchor pdfAnchor = new Anchor();

    public AnalyticsView(AnalyticsService analyticsService, ExaminationService examinationService) {
        this.analyticsService = analyticsService;
        this.examinationService = examinationService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Institutional Examination Analytics & Reports (FR12)");
        Paragraph sub = new Paragraph("Real-time database aggregated result metrics, grade distribution, course analytics, and exportable Result Passing Board (RPB) dossiers.");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("380px");

        examSelect.addValueChangeListener(e -> refreshAnalytics());

        // Export Buttons
        Button exportCsvBtn = new Button("Export RPB Report (CSV)", VaadinIcon.FILE_TABLE.create());
        exportCsvBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        csvAnchor.add(exportCsvBtn);
        csvAnchor.getElement().setAttribute("download", true);

        Button exportPdfBtn = new Button("Export RPB Report (PDF)", VaadinIcon.FILE_TEXT.create());
        exportPdfBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        pdfAnchor.add(exportPdfBtn);
        pdfAnchor.getElement().setAttribute("download", true);

        bar.add(examSelect, csvAnchor, pdfAnchor);

        kpiRow.setWidthFull();
        chartsRow.setWidthFull();

        configureCourseGrid();

        add(title, sub, bar, kpiRow, chartsRow, new H3("Course-Wise Performance Breakdown (RPB Format)"), courseGrid);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void configureCourseGrid() {
        courseGrid.setWidthFull();
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::courseCode).setHeader("Course Code").setWidth("130px").setSortable(true);
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::courseTitle).setHeader("Course Title").setFlexGrow(2).setSortable(true);
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::appeared).setHeader("Appeared").setWidth("100px");
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::passed).setHeader("Passed").setWidth("100px");
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::failed).setHeader("Failed").setWidth("100px");
        courseGrid.addColumn(r -> r.passPercentage() + "%").setHeader("Pass %").setWidth("110px").setSortable(true);
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::highestMark).setHeader("Highest").setWidth("110px");
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::lowestMark).setHeader("Lowest").setWidth("110px");
        courseGrid.addColumn(AnalyticsService.CourseAnalysisRow::averageMark).setHeader("Average").setWidth("110px");
    }

    private void refreshAnalytics() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        // Update KPI Cards
        AnalyticsService.DashboardOverview ov = analyticsService.getDashboardOverview(exam.getId());
        kpiRow.removeAll();
        kpiRow.add(createCard("Total Candidates", String.valueOf(ov.totalPublished()), VaadinIcon.USERS, "primary"));
        kpiRow.add(createCard("Passed Candidates", String.valueOf(ov.passedCount()), VaadinIcon.CHECK_CIRCLE, "success"));
        kpiRow.add(createCard("Arrear Candidates", String.valueOf(ov.failedCount()), VaadinIcon.WARNING, "error"));
        kpiRow.add(createCard("Overall Pass Rate", ov.passPercentage() + "%", VaadinIcon.TROPHY, "primary"));

        // Render JFreeChart PNGs
        chartsRow.removeAll();

        VerticalLayout barChartBox = new VerticalLayout();
        barChartBox.setWidth("50%");
        barChartBox.getStyle().set("background", "var(--lumo-base-color)").set("border-radius", "8px").set("border", "1px solid var(--lumo-contrast-10pct)").set("padding", "10px");
        String barPng = analyticsService.generateGradeDistributionPngDataUri(exam.getId(), 500, 300);
        Image barImg = new Image(barPng, "Grade Distribution");
        barImg.setWidthFull();
        barChartBox.add(barImg);

        VerticalLayout pieChartBox = new VerticalLayout();
        pieChartBox.setWidth("50%");
        pieChartBox.getStyle().set("background", "var(--lumo-base-color)").set("border-radius", "8px").set("border", "1px solid var(--lumo-contrast-10pct)").set("padding", "10px");
        String piePng = analyticsService.generatePassFailPieDataUri(exam.getId(), 500, 300);
        Image pieImg = new Image(piePng, "Pass vs Fail Ratio");
        pieImg.setWidthFull();
        pieChartBox.add(pieImg);

        chartsRow.add(barChartBox, pieChartBox);

        // Course Analysis Grid
        List<AnalyticsService.CourseAnalysisRow> rows = analyticsService.getCourseAnalysisForExam(exam.getId());
        courseGrid.setItems(rows);

        // Configure Export Links
        csvAnchor.setHref(new StreamResource("RPB_Analysis_Report_" + exam.getId() + ".csv",
                () -> new ByteArrayInputStream(analyticsService.exportRpbReportCsv(exam.getId()).getBytes(StandardCharsets.UTF_8))));

        pdfAnchor.setHref(new StreamResource("RPB_Analysis_Report_" + exam.getId() + ".pdf",
                () -> new ByteArrayInputStream(analyticsService.exportRpbReportPdf(exam.getId()))));
    }

    private VerticalLayout createCard(String title, String val, VaadinIcon icon, String theme) {
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

        H2 v = new H2(val);
        v.getStyle().set("margin", "6px 0 0 0");

        card.add(top, v);
        return card;
    }
}
