package com.erps.view;

import com.erps.audit.AuditService;
import com.erps.entity.GradeScale;
import com.erps.entity.GradingConfig;
import com.erps.entity.Regulation;
import com.erps.repository.GradeScaleRepository;
import com.erps.repository.GradingConfigRepository;
import com.erps.service.MasterDataService;
import com.erps.service.ResultService;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "grading-config", layout = MainLayout.class)
@PageTitle("Grading Rules & Regulation Scales | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class GradingConfigView extends VerticalLayout {

    private final MasterDataService masterDataService;
    private final GradeScaleRepository gradeScaleRepository;
    private final GradingConfigRepository gradingConfigRepository;
    private final ResultService resultService;
    private final AuditService auditService;

    private final Select<Regulation> regSelect = new Select<>();
    private final Grid<GradeScale> scaleGrid = new Grid<>(GradeScale.class, false);
    private final VerticalLayout configSection = new VerticalLayout();

    public GradingConfigView(MasterDataService masterDataService,
                             GradeScaleRepository gradeScaleRepository,
                             GradingConfigRepository gradingConfigRepository,
                             ResultService resultService,
                             AuditService auditService) {
        this.masterDataService = masterDataService;
        this.gradeScaleRepository = gradeScaleRepository;
        this.gradingConfigRepository = gradingConfigRepository;
        this.resultService = resultService;
        this.auditService = auditService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Autonomous College Grading Configuration & Scales (FR14)");

        // VERIFY Advisory Banner
        Div banner = new Div();
        banner.getStyle()
                .set("background", "var(--lumo-warning-color-10pct)")
                .set("border", "1px solid var(--lumo-warning-color-50pct)")
                .set("border-radius", "8px")
                .set("padding", "12px 16px")
                .set("color", "var(--lumo-warning-text-color)");
        Span bText = new Span("⚠️ VERIFY AGAINST REGULATION: Grade points and marks boundaries seeded below reflect the institutional UG Regulation absolute grading template. Controllers may edit intervals and grade points here. Modifying rules requires an explicit result recalculation.");
        bText.getStyle().set("font-weight", "500");
        banner.add(bText);

        HorizontalLayout bar = new HorizontalLayout();
        regSelect.setLabel("Select Regulation Batch");
        regSelect.setItemLabelGenerator(r -> r.getCode() + " - " + r.getName() + " (Batch " + r.getEffectiveFromBatch() + "+)");
        regSelect.setItems(masterDataService.getAllRegulations());
        regSelect.setWidth("420px");

        regSelect.addValueChangeListener(e -> refreshView());

        Button addScaleBtn = new Button("Add Grade Interval", VaadinIcon.PLUS.create(), e -> openScaleDialog(null));
        addScaleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(regSelect, addScaleBtn);

        configureScaleGrid();

        add(title, banner, bar, scaleGrid, configSection);

        if (!masterDataService.getAllRegulations().isEmpty()) {
            regSelect.setValue(masterDataService.getAllRegulations().get(0));
        }
    }

    private void configureScaleGrid() {
        scaleGrid.setWidthFull();
        scaleGrid.addColumn(GradeScale::getGrade).setHeader("Letter Grade").setWidth("140px").setSortable(true);
        scaleGrid.addColumn(s -> s.getMinMarks() + " - " + s.getMaxMarks()).setHeader("Marks Range (Absolute Scale)").setWidth("220px");
        scaleGrid.addColumn(GradeScale::getGradePoint).setHeader("Grade Point [VERIFY]").setWidth("180px").setSortable(true);

        scaleGrid.addComponentColumn(s -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openScaleDialog(s));
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return editBtn;
        }).setHeader("Edit Interval").setWidth("120px");
    }

    private void refreshView() {
        Regulation reg = regSelect.getValue();
        if (reg == null) return;

        scaleGrid.setItems(gradeScaleRepository.findByRegulationIdOrderByMinMarksDesc(reg.getId()));
        buildRegulationConfigSection(reg);
    }

    private void buildRegulationConfigSection(Regulation reg) {
        configSection.removeAll();
        configSection.setWidthFull();

        GradingConfig config = gradingConfigRepository.findByRegulationId(reg.getId())
                .orElseGet(() -> gradingConfigRepository.save(new GradingConfig(reg)));

        H3 cfgTitle = new H3("Regulation Policy Rules & Operational Thresholds");

        Checkbox rankCheck = new Checkbox("Class Ranking requires Zero Arrears (FR13)", Boolean.TRUE.equals(config.getRankRequiresNoArrear()));
        Checkbox minSeeCheck = new Checkbox("Enforce Minimum SEE marks for course pass (default disabled)", Boolean.TRUE.equals(config.getMinSeeMarksEnabled()));
        BigDecimalField minSeeField = new BigDecimalField("Default Min SEE Marks Threshold");
        minSeeField.setValue(config.getDefaultMinSeeMarks());
        minSeeField.setEnabled(Boolean.TRUE.equals(config.getMinSeeMarksEnabled()));

        minSeeCheck.addValueChangeListener(e -> minSeeField.setEnabled(e.getValue()));

        IntegerField revalDaysField = new IntegerField("Revaluation Window Duration (Days) [VERIFY: 15 days]");
        revalDaysField.setValue(config.getRevalWindowDays());

        IntegerField publishSlaField = new IntegerField("Result Publication Target SLA (Days) [3 days]");
        publishSlaField.setValue(config.getPublishSlaDays());

        FormLayout form = new FormLayout(rankCheck, minSeeCheck, minSeeField, revalDaysField, publishSlaField);

        Button saveCfgBtn = new Button("Save Regulation Rules", VaadinIcon.CHECK.create(), e -> {
            config.setRankRequiresNoArrear(rankCheck.getValue());
            config.setMinSeeMarksEnabled(minSeeCheck.getValue());
            config.setDefaultMinSeeMarks(minSeeField.getValue());
            config.setRevalWindowDays(revalDaysField.getValue());
            config.setPublishSlaDays(publishSlaField.getValue());
            gradingConfigRepository.save(config);

            auditService.logAction("UPDATE_GRADING_CONFIG", "GradingConfig", config.getId().toString(),
                    null, "Updated rules for " + reg.getCode());
            Notification.show("Regulation rules updated successfully.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveCfgBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        configSection.add(cfgTitle, form, saveCfgBtn);
    }

    private void openScaleDialog(GradeScale existing) {
        Regulation reg = regSelect.getValue();
        if (reg == null) return;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add Grade Scale" : "Edit Grade: " + existing.getGrade());

        TextField gradeField = new TextField("Letter Grade (e.g. S, A+, A)");
        BigDecimalField minField = new BigDecimalField("Minimum Marks (inclusive)");
        BigDecimalField maxField = new BigDecimalField("Maximum Marks (inclusive)");
        IntegerField gpField = new IntegerField("Grade Point (0-10) [VERIFY]");

        if (existing != null) {
            gradeField.setValue(existing.getGrade());
            minField.setValue(existing.getMinMarks());
            maxField.setValue(existing.getMaxMarks());
            gpField.setValue(existing.getGradePoint());
        }

        FormLayout form = new FormLayout(gradeField, minField, maxField, gpField);
        dialog.add(form);

        Button saveBtn = new Button("Save Grade Interval", e -> {
            GradeScale scale = existing != null ? existing : new GradeScale();
            scale.setRegulation(reg);
            scale.setGrade(gradeField.getValue());
            scale.setMinMarks(minField.getValue());
            scale.setMaxMarks(maxField.getValue());
            scale.setGradePoint(gpField.getValue());

            gradeScaleRepository.save(scale);
            auditService.logAction("UPDATE_GRADE_SCALE", "GradeScale", reg.getCode(),
                    null, String.format("Grade %s: [%s - %s] -> GP %d", scale.getGrade(), scale.getMinMarks(), scale.getMaxMarks(), scale.getGradePoint()));

            Notification.show("Grade interval updated. Be sure to re-calculate exam results if modifying published boundaries.", 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            dialog.close();
            refreshView();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
}
