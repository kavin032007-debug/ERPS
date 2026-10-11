package com.erps.view;

import com.erps.entity.*;
import com.erps.repository.CourseRegistrationRepository;
import com.erps.repository.MarksRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.MarksService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.*;

@Route(value = "cia-entry", layout = MainLayout.class)
@PageTitle("CIA Internal Marks Entry | ERPS")
@RolesAllowed({"FACULTY", "EXAM_CELL_COORDINATOR", "SUPER_ADMIN"})
public class CiaMarksEntryView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final MarksService marksService;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final MarksRepository marksRepository;
    private final AuthenticatedUser authenticatedUser;

    private final Select<Examination> examSelect = new Select<>();
    private final Select<ExamCourse> courseSelect = new Select<>();
    private final Grid<StudentMarkRow> grid = new Grid<>();
    private final Checkbox verifiedCheckbox = new Checkbox("I confirm that these internal marks have been verified with and displayed to the students (FR4 Policy Requirement)");
    private final Map<Long, BigDecimalField> inputMap = new HashMap<>();

    public static class StudentMarkRow {
        private final Student student;
        private BigDecimal ciaMarks;
        private MarksStatus status;

        public StudentMarkRow(Student student, BigDecimal ciaMarks, MarksStatus status) {
            this.student = student;
            this.ciaMarks = ciaMarks;
            this.status = status;
        }

        public Student getStudent() { return student; }
        public BigDecimal getCiaMarks() { return ciaMarks; }
        public void setCiaMarks(BigDecimal ciaMarks) { this.ciaMarks = ciaMarks; }
        public MarksStatus getStatus() { return status; }
        public void setStatus(MarksStatus status) { this.status = status; }
    }

    public CiaMarksEntryView(ExaminationService examinationService,
                             MarksService marksService,
                             CourseRegistrationRepository courseRegistrationRepository,
                             MarksRepository marksRepository,
                             AuthenticatedUser authenticatedUser) {
        this.examinationService = examinationService;
        this.marksService = marksService;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.marksRepository = marksRepository;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("Continuous Internal Assessment (CIA) Marks Entry");

        HorizontalLayout selectorBar = new HorizontalLayout();
        selectorBar.setWidthFull();

        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("350px");

        courseSelect.setLabel("Select Course");
        courseSelect.setItemLabelGenerator(ec -> ec.getCourse().getCode() + " - " + ec.getCourse().getTitle() + " (Max: " + ec.getCourse().getCiaMax() + ")");
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

        courseSelect.addValueChangeListener(e -> loadStudentRows());

        selectorBar.add(examSelect, courseSelect);

        configureGrid();

        // Action Buttons Bar
        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setWidthFull();
        actionsBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Button saveDraftBtn = new Button("Save Draft", VaadinIcon.DISC.create(), e -> saveMarks(false));
        Button submitBtn = new Button("Submit Final CIA Marks", VaadinIcon.CHECK.create(), e -> saveMarks(true));
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button lockPublishBtn = new Button("Lock & Publish Internals", VaadinIcon.LOCK.create(), e -> lockAndPublishInternals());
        lockPublishBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        actionsBar.add(saveDraftBtn, submitBtn, lockPublishBtn);

        verifiedCheckbox.getStyle().set("font-weight", "500").set("color", "var(--lumo-primary-text-color)");

        add(title, selectorBar, verifiedCheckbox, grid, actionsBar);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(row -> row.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px").setFlexGrow(0);
        grid.addColumn(row -> row.getStudent().getName()).setHeader("Student Name").setFlexGrow(1);

        grid.addComponentColumn(row -> {
            BigDecimalField field = new BigDecimalField();
            field.setWidth("140px");
            if (row.getCiaMarks() != null) {
                field.setValue(row.getCiaMarks());
            }
            inputMap.put(row.getStudent().getId(), field);
            return field;
        }).setHeader("CIA Marks").setWidth("180px");

        grid.addComponentColumn(row -> {
            Span badge = new Span(row.getStatus() != null ? row.getStatus().name() : "DRAFT");
            badge.getElement().getThemeList().add(row.getStatus() == MarksStatus.FINAL || row.getStatus() == MarksStatus.SUBMITTED ? "badge success small" : "badge contrast small");
            return badge;
        }).setHeader("Status").setWidth("140px");
    }

    private void loadStudentRows() {
        inputMap.clear();
        ExamCourse ec = courseSelect.getValue();
        if (ec == null) {
            grid.setItems(Collections.emptyList());
            return;
        }

        List<CourseRegistration> regs = courseRegistrationRepository.findByExamIdAndCourseId(
                ec.getExamination().getId(), ec.getCourse().getId());

        List<StudentMarkRow> rows = new ArrayList<>();
        for (CourseRegistration cr : regs) {
            Optional<Marks> mOpt = marksRepository.findByExamCourseIdAndStudentId(ec.getId(), cr.getStudent().getId());
            BigDecimal mark = mOpt.map(Marks::getCiaMarks).orElse(null);
            MarksStatus st = mOpt.map(Marks::getStatus).orElse(MarksStatus.DRAFT);
            rows.add(new StudentMarkRow(cr.getStudent(), mark, st));
        }

        grid.setItems(rows);
    }

    private void saveMarks(boolean isFinal) {
        ExamCourse ec = courseSelect.getValue();
        if (ec == null) return;

        if (isFinal && !verifiedCheckbox.getValue()) {
            Notification.show("You must check 'Verified with students' prior to final submission.", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String user = authenticatedUser.get().map(User::getEmail).orElse("faculty@institution.edu");
        String role = authenticatedUser.get().map(u -> u.getRole().name()).orElse("FACULTY");

        int count = 0;
        try {
            for (Map.Entry<Long, BigDecimalField> entry : inputMap.entrySet()) {
                Long studentId = entry.getKey();
                BigDecimal val = entry.getValue().getValue();
                Student student = courseRegistrationRepository.findByStudentId(studentId).get(0).getStudent();

                marksService.saveCiaMarks(ec, student, val, isFinal, verifiedCheckbox.getValue(), user, role);
                count++;
            }
            Notification.show(isFinal ? "Submitted CIA marks for " + count + " students." : "Draft saved for " + count + " students.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadStudentRows();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void lockAndPublishInternals() {
        ExamCourse ec = courseSelect.getValue();
        if (ec == null) return;

        String user = authenticatedUser.get().map(User::getEmail).orElse("coordinator@institution.edu");
        try {
            marksService.lockAndPublishInternalMarks(ec.getId(), user);
            Notification.show("Internal marks locked and published to students.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
