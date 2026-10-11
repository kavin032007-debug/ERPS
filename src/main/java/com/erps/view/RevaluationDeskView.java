package com.erps.view;

import com.erps.entity.Marks;
import com.erps.entity.RevaluationRequest;
import com.erps.entity.RevaluationStatus;
import com.erps.entity.User;
import com.erps.repository.MarksRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.RevaluationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route(value = "revaluation", layout = MainLayout.class)
@PageTitle("Revaluation Desk | ERPS")
@RolesAllowed({"TECH_STAFF", "SUPER_ADMIN"})
public class RevaluationDeskView extends VerticalLayout {

    private final RevaluationService revaluationService;
    private final MarksRepository marksRepository;
    private final ExaminationService examinationService;
    private final AuthenticatedUser authenticatedUser;

    private final Grid<RevaluationRequest> grid = new Grid<>(RevaluationRequest.class, false);

    public RevaluationDeskView(RevaluationService revaluationService,
                               MarksRepository marksRepository,
                               ExaminationService examinationService,
                               AuthenticatedUser authenticatedUser) {
        this.revaluationService = revaluationService;
        this.marksRepository = marksRepository;
        this.examinationService = examinationService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Examination Office: Revaluation Processing Desk (FR11)");
        Paragraph desc = new Paragraph("Policy Workflow: Fee payment verification, answer-script photocopy tracking, revaluation valuation entry, and sign-off queue integration. 30-day resolution SLA.");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configureGrid();
        refreshGrid();

        add(title, desc, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(r -> r.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px");
        grid.addColumn(r -> r.getStudent().getName()).setHeader("Student Name").setFlexGrow(1);
        grid.addColumn(r -> r.getCourse().getCode()).setHeader("Course").setWidth("120px");
        grid.addColumn(RevaluationRequest::getReason).setHeader("Student Justification").setFlexGrow(2);

        grid.addComponentColumn(r -> {
            Checkbox feeCheck = new Checkbox("Fee Paid", Boolean.TRUE.equals(r.getFeePaidFlag()));
            feeCheck.addValueChangeListener(e -> {
                String staff = authenticatedUser.get().map(User::getFullName).orElse("Tech Staff");
                revaluationService.recordFeePaymentAndRegister(r.getId(), e.getValue(), staff);
                Notification.show("Fee status updated.");
            });
            return feeCheck;
        }).setHeader("Fee Verification").setWidth("150px");

        grid.addComponentColumn(r -> {
            Span badge = new Span(r.getStatus().name());
            badge.getElement().getThemeList().add(r.getStatus() == RevaluationStatus.COMPLETED ? "badge success small" : "badge primary small");
            return badge;
        }).setHeader("Status").setWidth("140px");

        grid.addComponentColumn(r -> {
            if (revaluationService.isRevaluationOverdue(r, 30)) {
                Span overdue = new Span("⚠️ Overdue (>30d)");
                overdue.getElement().getThemeList().add("badge error small");
                return overdue;
            }
            Span onTime = new Span("Within 30d SLA");
            onTime.getElement().getThemeList().add("badge success small");
            return onTime;
        }).setHeader("SLA Target").setWidth("140px");

        grid.addComponentColumn(this::createActionButtons).setHeader("Desk Actions").setWidth("220px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(RevaluationRequest req) {
        HorizontalLayout layout = new HorizontalLayout();

        if (req.getStatus() != RevaluationStatus.COMPLETED) {
            Button valBtn = new Button("Enter Revaluation", VaadinIcon.EDIT.create(), e -> openValuationDialog(req));
            valBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            layout.add(valBtn);

            if (Boolean.TRUE.equals(req.getPhotocopyRequested())) {
                Button photoBtn = new Button("Photocopy Ready", VaadinIcon.PRINT.create(), e -> {
                    revaluationService.markPhotocopyAvailable(req.getId());
                    Notification.show("Photocopy marked available for student pickup.");
                    refreshGrid();
                });
                photoBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                layout.add(photoBtn);
            }
        } else {
            layout.add(new Span("Completed"));
        }

        return layout;
    }

    private void openValuationDialog(RevaluationRequest req) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Revaluation Valuation: " + req.getCourse().getCode() + " for " + req.getStudent().getRegisterNo());

        BigDecimalField newMarkField = new BigDecimalField("Revised SEE Marks");
        newMarkField.setValue(req.getOldMarks() != null ? req.getOldMarks().add(BigDecimal.valueOf(5.0)) : BigDecimal.valueOf(50.0));

        dialog.add(new FormLayout(newMarkField));

        Button saveBtn = new Button("Submit for Mark-Alteration Sign-Off", e -> {
            try {
                String examiner = authenticatedUser.get().map(User::getFullName).orElse("Senior Examiner");
                Long examCourseId = examinationService.getExamCourse(req.getExam().getId(), req.getCourse().getId())
                        .orElseThrow().getId();
                Marks marks = marksRepository.findByExamCourseIdAndStudentId(examCourseId, req.getStudent().getId())
                        .orElseThrow();

                revaluationService.completeRevaluationValuation(req.getId(), newMarkField.getValue(), marks.getId(), examiner);

                Notification.show("Revaluation recorded! Forwarded to CoE Mark-Alteration queue for sign-off.",
                        5000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(revaluationService.getAllRequests());
    }
}
