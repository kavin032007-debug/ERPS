package com.erps.view;

import com.erps.entity.*;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.MarksService;
import com.erps.service.ModerationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "moderation", layout = MainLayout.class)
@PageTitle("Moderation & Grace Marks | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class ModerationView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final ModerationService moderationService;
    private final MarksService marksService;
    private final AuthenticatedUser authenticatedUser;

    private final Select<Examination> examSelect = new Select<>();
    private final Select<ExamCourse> courseSelect = new Select<>();
    private final Grid<Marks> marksGrid = new Grid<>(Marks.class, false);

    public ModerationView(ExaminationService examinationService,
                          ModerationService moderationService,
                          MarksService marksService,
                          AuthenticatedUser authenticatedUser) {
        this.examinationService = examinationService;
        this.moderationService = moderationService;
        this.marksService = marksService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("Board of Examinations: Approved Course Moderation (Grace Marks)");
        Paragraph desc = new Paragraph("Policy Rule (FR6): Moderation may be applied course-wise prior to Result Passing Board approval. Original marks and moderated marks are both permanently retained in the database.");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout selectorBar = new HorizontalLayout();
        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("350px");

        courseSelect.setLabel("Select Course for Moderation");
        courseSelect.setItemLabelGenerator(ec -> ec.getCourse().getCode() + " - " + ec.getCourse().getTitle());
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

        courseSelect.addValueChangeListener(e -> refreshMarks());
        selectorBar.add(examSelect, courseSelect);

        // Moderation Input Bar
        HorizontalLayout modBar = new HorizontalLayout();
        modBar.setWidthFull();
        modBar.setAlignItems(Alignment.BASELINE);

        BigDecimalField graceField = new BigDecimalField("Grace Marks (+)");
        graceField.setValue(BigDecimal.valueOf(2.00));
        graceField.setWidth("150px");

        TextField reasonField = new TextField("Moderation Committee Justification");
        reasonField.setWidth("450px");
        reasonField.setPlaceholder("e.g. Approved by Moderation Committee for question ambiguity in Q12");

        Button applyBtn = new Button("Apply Moderation", VaadinIcon.MAGIC.create(), e -> {
            ExamCourse ec = courseSelect.getValue();
            if (ec == null) return;
            try {
                String user = authenticatedUser.get().map(User::getFullName).orElse("Super Admin");
                int count = moderationService.applyCourseModeration(ec.getId(), graceField.getValue(), reasonField.getValue(), user);
                Notification.show("Applied +" + graceField.getValue() + " grace marks to " + count + " candidates.", 4000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshMarks();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        applyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        modBar.add(graceField, reasonField, applyBtn);

        configureGrid();
        refreshMarks();

        add(title, desc, selectorBar, modBar, marksGrid);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void configureGrid() {
        marksGrid.setSizeFull();
        marksGrid.addColumn(m -> m.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px");
        marksGrid.addColumn(m -> m.getStudent().getName()).setHeader("Student Name").setFlexGrow(1);
        marksGrid.addColumn(m -> m.getCiaMarks() != null ? m.getCiaMarks().toPlainString() : "-").setHeader("CIA Marks").setWidth("120px");
        marksGrid.addColumn(m -> m.getSeeMarks() != null ? m.getSeeMarks().toPlainString() : "-").setHeader("Original SEE Marks").setWidth("150px");
        marksGrid.addColumn(m -> m.getModeratedMarks() != null ? m.getModeratedMarks().toPlainString() : "None").setHeader("Moderated SEE Marks").setWidth("170px");
        marksGrid.addColumn(Marks::getFlag).setHeader("Flag").setWidth("120px");
        marksGrid.addColumn(Marks::getStatus).setHeader("Status").setWidth("140px");
    }

    private void refreshMarks() {
        ExamCourse ec = courseSelect.getValue();
        if (ec != null) {
            marksGrid.setItems(marksService.getMarksForExamCourse(ec.getId()));
        } else {
            marksGrid.setItems(List.of());
        }
    }
}
