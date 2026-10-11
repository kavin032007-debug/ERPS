package com.erps.view;

import com.erps.entity.*;
import com.erps.repository.CourseRegistrationRepository;
import com.erps.repository.MarksRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.DummyIdService;
import com.erps.service.ExaminationService;
import com.erps.service.MarksService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.*;

@Route(value = "see-entry", layout = MainLayout.class)
@PageTitle("SEE Marks Entry (Dummy-ID Masked) | ERPS")
@RolesAllowed({"FACULTY", "SUPER_ADMIN"})
public class SeeMarksEntryView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final MarksService marksService;
    private final DummyIdService dummyIdService;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final MarksRepository marksRepository;
    private final AuthenticatedUser authenticatedUser;

    private final Select<Examination> examSelect = new Select<>();
    private final Select<ExamCourse> courseSelect = new Select<>();
    private final Grid<SeeMarkRow> grid = new Grid<>();
    private final Map<String, RowFields> inputMap = new HashMap<>();

    public static class RowFields {
        BigDecimalField markField;
        Select<MarksFlag> flagSelect;

        public RowFields(BigDecimalField markField, Select<MarksFlag> flagSelect) {
            this.markField = markField;
            this.flagSelect = flagSelect;
        }
    }

    public static class SeeMarkRow {
        private final DummyMapping dummyMapping;
        private BigDecimal seeMarks;
        private MarksFlag flag;
        private MarksStatus status;

        public SeeMarkRow(DummyMapping dummyMapping, BigDecimal seeMarks, MarksFlag flag, MarksStatus status) {
            this.dummyMapping = dummyMapping;
            this.seeMarks = seeMarks;
            this.flag = flag != null ? flag : MarksFlag.NONE;
            this.status = status;
        }

        public DummyMapping getDummyMapping() { return dummyMapping; }
        public String getDummyId() { return dummyMapping.getDummyId(); }
        public BigDecimal getSeeMarks() { return seeMarks; }
        public MarksFlag getFlag() { return flag; }
        public MarksStatus getStatus() { return status; }
    }

    public SeeMarksEntryView(ExaminationService examinationService,
                             MarksService marksService,
                             DummyIdService dummyIdService,
                             CourseRegistrationRepository courseRegistrationRepository,
                             MarksRepository marksRepository,
                             AuthenticatedUser authenticatedUser) {
        this.examinationService = examinationService;
        this.marksService = marksService;
        this.dummyIdService = dummyIdService;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.marksRepository = marksRepository;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("Semester End Examination (SEE) Marks Entry - Dummy-ID Blind Valuation");
        Paragraph notice = new Paragraph("CONFIDENTIALITY PROTOCOL: Student Register Numbers and Names are strictly masked under system-generated Dummy IDs. Valuation is blind.");
        notice.getStyle().set("color", "var(--lumo-primary-text-color)").set("font-weight", "500");

        HorizontalLayout selectorBar = new HorizontalLayout();
        selectorBar.setWidthFull();

        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("350px");

        courseSelect.setLabel("Select Course");
        courseSelect.setItemLabelGenerator(ec -> ec.getCourse().getCode() + " - " + ec.getCourse().getTitle() + " (SEE Max: " + ec.getCourse().getSeeMax() + ")");
        courseSelect.setWidth("450px");

        examSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                List<ExamCourse> courses = examinationService.getExamCourses(e.getValue().getId());
                courseSelect.setItems(courses);
                if (!courses.isEmpty()) {
                    courseSelect.setValue(courses.get(0));
                }
            }
        });

        courseSelect.addValueChangeListener(e -> loadDummyRows());

        selectorBar.add(examSelect, courseSelect);

        configureGrid();

        HorizontalLayout actionsBar = new HorizontalLayout();
        Button saveDraftBtn = new Button("Save Draft Evaluation", VaadinIcon.DISC.create(), e -> saveSeeMarks(false));
        Button submitBtn = new Button("Finalize & Submit Valuation", VaadinIcon.CHECK_CIRCLE.create(), e -> saveSeeMarks(true));
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionsBar.add(saveDraftBtn, submitBtn);

        add(title, notice, selectorBar, grid, actionsBar);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(SeeMarkRow::getDummyId).setHeader("Confidential Dummy ID").setWidth("220px").setFlexGrow(0).setSortable(true);

        grid.addComponentColumn(row -> {
            BigDecimalField field = new BigDecimalField();
            field.setWidth("140px");
            if (row.getSeeMarks() != null) {
                field.setValue(row.getSeeMarks());
            }

            Select<MarksFlag> flagSel = new Select<>();
            flagSel.setWidth("160px");
            flagSel.setItems(MarksFlag.values());
            flagSel.setValue(row.getFlag());

            flagSel.addValueChangeListener(ev -> {
                if (ev.getValue() == MarksFlag.ABSENT || ev.getValue() == MarksFlag.MALPRACTICE) {
                    field.setValue(BigDecimal.ZERO);
                    field.setEnabled(false);
                } else {
                    field.setEnabled(true);
                }
            });

            inputMap.put(row.getDummyId(), new RowFields(field, flagSel));

            HorizontalLayout hl = new HorizontalLayout(field, flagSel);
            hl.setSpacing(true);
            return hl;
        }).setHeader("SEE Marks & Evaluation Flag").setWidth("340px");

        grid.addComponentColumn(row -> {
            Span badge = new Span(row.getStatus() != null ? row.getStatus().name() : "DRAFT");
            badge.getElement().getThemeList().add(row.getStatus() == MarksStatus.FINAL || row.getStatus() == MarksStatus.SUBMITTED ? "badge success small" : "badge contrast small");
            return badge;
        }).setHeader("Status").setWidth("140px");
    }

    private void loadDummyRows() {
        inputMap.clear();
        ExamCourse ec = courseSelect.getValue();
        if (ec == null) {
            grid.setItems(Collections.emptyList());
            return;
        }

        List<CourseRegistration> regs = courseRegistrationRepository.findByExamIdAndCourseId(
                ec.getExamination().getId(), ec.getCourse().getId());

        List<Student> students = regs.stream().map(CourseRegistration::getStudent).toList();
        List<DummyMapping> mappings = dummyIdService.generateOrGetDummyMappings(ec.getExamination(), ec.getCourse(), students);

        List<SeeMarkRow> rows = new ArrayList<>();
        for (DummyMapping dm : mappings) {
            Optional<Marks> mOpt = marksRepository.findByExamCourseIdAndStudentId(ec.getId(), dm.getStudent().getId());
            BigDecimal mark = mOpt.map(Marks::getSeeMarks).orElse(null);
            MarksFlag flag = mOpt.map(Marks::getFlag).orElse(MarksFlag.NONE);
            MarksStatus st = mOpt.map(Marks::getStatus).orElse(MarksStatus.DRAFT);
            rows.add(new SeeMarkRow(dm, mark, flag, st));
        }

        grid.setItems(rows);
    }

    private void saveSeeMarks(boolean isFinal) {
        ExamCourse ec = courseSelect.getValue();
        if (ec == null) return;

        String user = authenticatedUser.get().map(User::getEmail).orElse("examiner@institution.edu");
        String role = authenticatedUser.get().map(u -> u.getRole().name()).orElse("FACULTY");

        int count = 0;
        try {
            for (Map.Entry<String, RowFields> entry : inputMap.entrySet()) {
                String dummyId = entry.getKey();
                RowFields fields = entry.getValue();

                Optional<Student> studentOpt = dummyIdService.deMask(ec.getExamination().getId(), ec.getCourse().getId(), dummyId, user, role);
                if (studentOpt.isEmpty()) continue;

                Student s = studentOpt.get();
                BigDecimal val = fields.markField.getValue();
                MarksFlag flag = fields.flagSelect.getValue();

                marksService.saveSeeMarks(ec, s, val, flag, isFinal, user, role);
                count++;
            }
            Notification.show(isFinal ? "Valuation submitted for " + count + " answer scripts." : "Evaluation draft saved for " + count + " scripts.",
                    3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadDummyRows();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
