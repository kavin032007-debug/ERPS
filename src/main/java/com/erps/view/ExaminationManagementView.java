package com.erps.view;

import com.erps.entity.*;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "examinations", layout = MainLayout.class)
@PageTitle("Examination Sessions & Deadlines | ERPS")
@RolesAllowed({"SUPER_ADMIN", "EXAM_CELL_COORDINATOR", "TECH_STAFF"})
public class ExaminationManagementView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final MasterDataService masterDataService;
    private final AuthenticatedUser authenticatedUser;

    private final Grid<Examination> examGrid = new Grid<>(Examination.class, false);
    private final Grid<ExamCourse> courseDeadlineGrid = new Grid<>(ExamCourse.class, false);

    public ExaminationManagementView(ExaminationService examinationService,
                                     MasterDataService masterDataService,
                                     AuthenticatedUser authenticatedUser) {
        this.examinationService = examinationService;
        this.masterDataService = masterDataService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        HorizontalLayout bar = new HorizontalLayout();
        Button createBtn = new Button("Create New Examination Session", VaadinIcon.PLUS.create(), e -> openCreateExamDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(createBtn);

        examGrid.addColumn(Examination::getName).setHeader("Examination Name").setFlexGrow(2).setSortable(true);
        examGrid.addColumn(Examination::getAcademicYear).setHeader("Academic Year").setWidth("140px");
        examGrid.addColumn(e -> "Sem " + e.getSemester()).setHeader("Semester").setWidth("100px");
        examGrid.addColumn(e -> e.getRegulation() != null ? e.getRegulation().getCode() : "-").setHeader("Regulation").setWidth("120px");

        examGrid.addComponentColumn(e -> {
            Span statusBadge = new Span(e.getStatus().name());
            String theme = switch (e.getStatus()) {
                case PUBLISHED -> "badge success small";
                case SEE_ENTRY, CIA_ENTRY -> "badge primary small";
                case LOCKED -> "badge error small";
                default -> "badge contrast small";
            };
            statusBadge.getElement().getThemeList().add(theme);
            return statusBadge;
        }).setHeader("Status").setWidth("140px");

        examGrid.addComponentColumn(this::createActionButtons).setHeader("Session Actions").setWidth("280px").setFlexGrow(0);

        examGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                refreshCourseDeadlines(e.getValue().getId());
            }
        });

        // Deadlines Grid
        H3 deadlinesTitle = new H3("Course Submission Deadlines for Selected Examination");
        configureDeadlineGrid();

        refreshExams();
        add(bar, examGrid, deadlinesTitle, courseDeadlineGrid);
    }

    private HorizontalLayout createActionButtons(Examination exam) {
        HorizontalLayout actions = new HorizontalLayout();

        Select<ExamStatus> statusSelect = new Select<>();
        statusSelect.setItems(ExamStatus.values());
        statusSelect.setValue(exam.getStatus());
        statusSelect.setWidth("140px");
        statusSelect.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null && e.getValue() != exam.getStatus()) {
                examinationService.updateStatus(exam.getId(), e.getValue());
                Notification.show("Exam status updated to " + e.getValue());
                refreshExams();
            }
        });

        Button unlockBtn = new Button("Unlock", VaadinIcon.UNLOCK.create(), e -> openUnlockDialog(exam));
        unlockBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        unlockBtn.setTooltipText("Unlock locked exam with mandatory justification");

        actions.add(statusSelect, unlockBtn);
        return actions;
    }

    private void configureDeadlineGrid() {
        courseDeadlineGrid.addColumn(ec -> ec.getCourse().getCode()).setHeader("Course Code").setWidth("130px").setFlexGrow(0);
        courseDeadlineGrid.addColumn(ec -> ec.getCourse().getTitle()).setHeader("Course Title").setFlexGrow(1);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        courseDeadlineGrid.addColumn(ec -> ec.getCiaDeadline() != null ? ec.getCiaDeadline().format(dtf) : "None")
                .setHeader("CIA Deadline").setWidth("170px");
        courseDeadlineGrid.addColumn(ec -> ec.getSeeDeadline() != null ? ec.getSeeDeadline().format(dtf) : "None")
                .setHeader("SEE Deadline").setWidth("170px");
        courseDeadlineGrid.addColumn(ec -> Boolean.TRUE.equals(ec.getInternalLocked()) ? "Locked" : "Open")
                .setHeader("Internals Lock").setWidth("130px");

        courseDeadlineGrid.addComponentColumn(ec -> {
            Button editBtn = new Button(VaadinIcon.CALENDAR.create(), e -> openDeadlineDialog(ec));
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return editBtn;
        }).setHeader("Edit Deadlines").setWidth("130px");
    }

    private void openCreateExamDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Examination Session");

        TextField nameField = new TextField("Examination Name (e.g. Nov/Dec 2026 End Semester)");
        TextField yearField = new TextField("Academic Year", "2025-2026", "");
        IntegerField semField = new IntegerField("Semester (e.g. 3)");
        semField.setValue(3);

        Select<Regulation> regSelect = new Select<>();
        regSelect.setLabel("Regulation");
        regSelect.setItemLabelGenerator(Regulation::getName);
        regSelect.setItems(masterDataService.getAllRegulations());

        FormLayout form = new FormLayout(nameField, yearField, semField, regSelect);
        dialog.add(form);

        Button save = new Button("Create Session", e -> {
            examinationService.createExamination(nameField.getValue(), yearField.getValue(), semField.getValue(), regSelect.getValue());
            Notification.show("Examination session created with attached courses.");
            dialog.close();
            refreshExams();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openDeadlineDialog(ExamCourse ec) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Update Deadlines: " + ec.getCourse().getCode());

        DateTimePicker ciaPicker = new DateTimePicker("CIA Submission Deadline");
        ciaPicker.setValue(ec.getCiaDeadline());

        DateTimePicker seePicker = new DateTimePicker("SEE Submission Deadline");
        seePicker.setValue(ec.getSeeDeadline());

        dialog.add(new FormLayout(ciaPicker, seePicker));

        Button save = new Button("Save Deadlines", e -> {
            examinationService.updateDeadlines(ec.getId(), ciaPicker.getValue(), seePicker.getValue());
            Notification.show("Deadlines updated.");
            dialog.close();
            refreshCourseDeadlines(ec.getExamination().getId());
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openUnlockDialog(Examination exam) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Unlock Examination (CoE / DCoE Action)");

        TextArea reasonArea = new TextArea("Mandatory Justification Reason");
        reasonArea.setWidthFull();
        reasonArea.setPlaceholder("State explicit reason for reopening entry mode. This action is permanently audit-logged and alerts the CoE.");

        dialog.add(reasonArea);

        Button confirm = new Button("Confirm Unlock", e -> {
            try {
                String user = authenticatedUser.get().map(User::getFullName).orElse("Super Admin");
                examinationService.unlockExamination(exam.getId(), reasonArea.getValue(), user);
                Notification.show("Examination unlocked. CoE alert generated.", 4000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshExams();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void refreshExams() {
        List<Examination> exams = examinationService.getAllExaminations();
        examGrid.setItems(exams);
        if (!exams.isEmpty() && courseDeadlineGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>()).count() == 0) {
            refreshCourseDeadlines(exams.get(0).getId());
        }
    }

    private void refreshCourseDeadlines(Long examId) {
        courseDeadlineGrid.setItems(examinationService.getExamCourses(examId));
    }
}
