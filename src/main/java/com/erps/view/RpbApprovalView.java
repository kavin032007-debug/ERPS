package com.erps.view;

import com.erps.entity.*;
import com.erps.repository.RpbMeetingRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.AnalyticsService;
import com.erps.service.ExaminationService;
import com.erps.service.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Route(value = "rpb-approval", layout = MainLayout.class)
@PageTitle("Result Passing Board (RPB) Approval | ERPS")
@RolesAllowed({"PRINCIPAL", "SUPER_ADMIN"})
public class RpbApprovalView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final WorkflowService workflowService;
    private final AnalyticsService analyticsService;
    private final RpbMeetingRepository rpbMeetingRepository;
    private final AuthenticatedUser authenticatedUser;

    private final Select<Examination> examSelect = new Select<>();
    private final VerticalLayout statsContainer = new VerticalLayout();
    private final VerticalLayout statusCard = new VerticalLayout();

    public RpbApprovalView(ExaminationService examinationService,
                           WorkflowService workflowService,
                           AnalyticsService analyticsService,
                           RpbMeetingRepository rpbMeetingRepository,
                           AuthenticatedUser authenticatedUser) {
        this.examinationService = examinationService;
        this.workflowService = workflowService;
        this.analyticsService = analyticsService;
        this.rpbMeetingRepository = rpbMeetingRepository;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Result Passing Board (RPB) Sign-Off & Approval (FR7)");
        Paragraph sub = new Paragraph("Policy Requirement: Chaired by Principal (RPB Chairman) with Controller of Examinations (Member Secretary) and Affiliating University Nominee. Digital sign-off required prior to public release.");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout bar = new HorizontalLayout();
        examSelect.setLabel("Select Examination Session");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("400px");

        examSelect.addValueChangeListener(e -> refreshView());

        Button approveBtn = new Button("Convene Board & Sign-Off (RPB Digital Approval)", VaadinIcon.CHECK_CIRCLE.create(), e -> openSignOffDialog());
        approveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        bar.add(examSelect, approveBtn);

        add(title, sub, bar, statusCard, statsContainer);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void refreshView() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        statsContainer.removeAll();
        statusCard.removeAll();

        Optional<RpbMeeting> meetingOpt = rpbMeetingRepository.findByExamId(exam.getId());

        // Status Card
        statusCard.setWidthFull();
        statusCard.setPadding(true);
        statusCard.getStyle()
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-15pct)");

        if (meetingOpt.isPresent()) {
            RpbMeeting m = meetingOpt.get();
            statusCard.getStyle().set("background", "var(--lumo-success-color-10pct)");
            H3 approvedHeading = new H3("RPB Approval Status: APPROVED & SIGNED OFF");
            approvedHeading.getStyle().set("color", "var(--lumo-success-text-color)").set("margin", "0 0 10px 0");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
            Paragraph details = new Paragraph(String.format("Meeting Date: %s | Minutes Ref: %s | Signed by: %s\nBoard Members: %s\nUniversity Nominee: %s",
                    m.getMeetingDate().format(dtf), m.getMinutesRef(), m.getSignOffBy(), m.getMembers(),
                    m.getUniversityNominee() != null ? m.getUniversityNominee() : "None"));

            statusCard.add(approvedHeading, details);
        } else {
            statusCard.getStyle().set("background", "var(--lumo-contrast-5pct)");
            H3 pendingHeading = new H3("RPB Approval Status: PENDING BOARD CONVOCATION");
            pendingHeading.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin", "0 0 10px 0");
            Paragraph info = new Paragraph("The examination results have not yet received formal RPB sign-off. Publication is blocked.");
            statusCard.add(pendingHeading, info);
        }

        // Summary Stats Cards
        AnalyticsService.DashboardOverview ov = analyticsService.getDashboardOverview(exam.getId());
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();

        cards.add(createStatCard("Candidates Evaluated", String.valueOf(ov.totalPublished()), "primary"));
        cards.add(createStatCard("Passed Candidates", String.valueOf(ov.passedCount()), "success"));
        cards.add(createStatCard("Arrear Candidates", String.valueOf(ov.failedCount()), "error"));
        cards.add(createStatCard("Institutional Pass Rate", ov.passPercentage() + "%", "primary"));

        statsContainer.add(new H3("Board Executive Summary"), cards);
    }

    private void openSignOffDialog() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Result Passing Board (RPB) Digital Sign-Off");
        dialog.setWidth("650px");

        DateTimePicker datePicker = new DateTimePicker("Meeting Date & Time");
        datePicker.setValue(LocalDateTime.now());

        TextField minutesField = new TextField("Minutes Reference Code (e.g. RPB/2026/M01)");
        minutesField.setValue("RPB/2026/M01");

        TextField nomineeField = new TextField("Affiliating University Nominee Name");
        nomineeField.setValue("Dr. University Nominee, Dean of Affiliated Colleges");

        TextArea membersArea = new TextArea("Attending Board Members");
        membersArea.setValue("Dr. A. Principal (Chairman), Dr. CoE (Member Secretary), HOD-CSE, HOD-ECE");
        membersArea.setHeight("100px");

        FormLayout form = new FormLayout(datePicker, minutesField, nomineeField, membersArea);
        dialog.add(form);

        Button confirmBtn = new Button("Affix Board Signature & Approve", VaadinIcon.CHECK_CIRCLE.create(), e -> {
            try {
                String signer = authenticatedUser.get().map(User::getFullName).orElse("Principal / RPB Chairman");
                workflowService.approveRpb(exam.getId(), datePicker.getValue(), membersArea.getValue(),
                        nomineeField.getValue(), minutesField.getValue(), signer);

                Notification.show("Results successfully approved by the Result Passing Board! Ready for publication.",
                        5000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshView();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private VerticalLayout createStatCard(String label, String value, String theme) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        Span l = new Span(label);
        l.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

        H2 v = new H2(value);
        v.getStyle().set("margin", "8px 0 0 0");

        card.add(l, v);
        return card;
    }
}
