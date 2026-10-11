package com.erps.view;

import com.erps.entity.AlterationStatus;
import com.erps.entity.MarkAlteration;
import com.erps.security.AuthenticatedUser;
import com.erps.service.MarksService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "alterations", layout = MainLayout.class)
@PageTitle("Mark-Alteration Sign-Off Queue | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class MarkAlterationView extends VerticalLayout {

    private final MarksService marksService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<MarkAlteration> grid = new Grid<>(MarkAlteration.class, false);

    public MarkAlterationView(MarksService marksService, AuthenticatedUser authenticatedUser) {
        this.marksService = marksService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Post-Valuation Mark Alteration Sign-Off Queue (FR8)");
        Paragraph desc = new Paragraph("Policy Rule: Any change to a mark post-submission creates an alert record. Changes take effect on results only after formal sign-off with mandatory comments by the Controller of Examinations.");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        configureGrid();
        refreshGrid();

        add(title, desc, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(a -> a.getMarks().getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px");
        grid.addColumn(a -> a.getMarks().getStudent().getName()).setHeader("Student Name").setFlexGrow(1);
        grid.addColumn(a -> a.getMarks().getExamCourse().getCourse().getCode()).setHeader("Course").setWidth("120px");
        grid.addColumn(a -> a.getOldValue() != null ? a.getOldValue().toPlainString() : "-").setHeader("Original Mark").setWidth("130px");
        grid.addColumn(a -> a.getNewValue() != null ? a.getNewValue().toPlainString() : "-").setHeader("Proposed New Mark").setWidth("150px");
        grid.addColumn(MarkAlteration::getReason).setHeader("Justification Reason").setFlexGrow(2);
        grid.addColumn(MarkAlteration::getRequestedBy).setHeader("Requested By").setWidth("180px");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM HH:mm");
        grid.addColumn(a -> a.getCreatedAt() != null ? a.getCreatedAt().format(dtf) : "-").setHeader("Timestamp").setWidth("130px");

        grid.addComponentColumn(this::createActionButtons).setHeader("Sign-Off Actions").setWidth("220px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(MarkAlteration alteration) {
        HorizontalLayout layout = new HorizontalLayout();

        if (alteration.getStatus() == AlterationStatus.PENDING) {
            Button approveBtn = new Button("Approve", VaadinIcon.CHECK.create(), e -> openSignOffDialog(alteration, true));
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);

            Button rejectBtn = new Button("Reject", VaadinIcon.CLOSE.create(), e -> openSignOffDialog(alteration, false));
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

            layout.add(approveBtn, rejectBtn);
        } else {
            Span badge = new Span(alteration.getStatus().name());
            badge.getElement().getThemeList().add(alteration.getStatus() == AlterationStatus.APPROVED ? "badge success small" : "badge error small");
            layout.add(badge);
        }

        return layout;
    }

    private void openSignOffDialog(MarkAlteration alteration, boolean isApprove) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle((isApprove ? "Approve" : "Reject") + " Mark Alteration for " + alteration.getMarks().getStudent().getRegisterNo());

        TextArea commentArea = new TextArea("Mandatory Sign-Off Comment / Reason");
        commentArea.setWidthFull();
        commentArea.setPlaceholder("State explicit rationale for " + (isApprove ? "approving" : "rejecting") + " this mark modification.");

        dialog.add(commentArea);

        Button confirmBtn = new Button("Confirm " + (isApprove ? "Approval" : "Rejection"), e -> {
            try {
                String signer = authenticatedUser.get().map(u -> u.getFullName()).orElse("Controller of Examinations");
                marksService.signOffAlteration(alteration.getId(), isApprove, commentArea.getValue(), signer);
                Notification.show("Mark alteration successfully " + (isApprove ? "approved and applied to results." : "rejected."),
                        4000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, isApprove ? ButtonVariant.LUMO_SUCCESS : ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(marksService.getPendingAlterations());
    }
}
