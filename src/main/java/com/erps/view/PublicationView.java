package com.erps.view;

import com.erps.entity.Examination;
import com.erps.entity.SemesterResult;
import com.erps.entity.WorkflowStatus;
import com.erps.repository.RpbMeetingRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.ResultService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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

@Route(value = "publication", layout = MainLayout.class)
@PageTitle("Result Publication | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class PublicationView extends VerticalLayout {

    private final ExaminationService examinationService;
    private final com.erps.service.WorkflowService workflowService;
    private final ResultService resultService;
    private final RpbMeetingRepository rpbMeetingRepository;
    private final AuthenticatedUser authenticatedUser;

    private final Select<Examination> examSelect = new Select<>();
    private final Grid<SemesterResult> grid = new Grid<>(SemesterResult.class, false);
    private final HorizontalLayout slaIndicator = new HorizontalLayout();

    public PublicationView(ExaminationService examinationService,
                           com.erps.service.WorkflowService workflowService,
                           ResultService resultService,
                           RpbMeetingRepository rpbMeetingRepository,
                           AuthenticatedUser authenticatedUser) {
        this.examinationService = examinationService;
        this.workflowService = workflowService;
        this.resultService = resultService;
        this.rpbMeetingRepository = rpbMeetingRepository;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Official Result Publication to Student Portal (FR7)");
        Paragraph info = new Paragraph("Policy Rule: Publication is strictly atomic per examination session. Results can ONLY be published after formal Result Passing Board (RPB) approval. Target SLA is publication within 3 days of RPB approval.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout bar = new HorizontalLayout();
        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("400px");

        examSelect.addValueChangeListener(e -> refreshView());

        Button publishBtn = new Button("Publish All Results Now", VaadinIcon.GLOBE.create(), e -> executePublish());
        publishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        bar.add(examSelect, publishBtn);

        grid.setSizeFull();
        grid.addColumn(sr -> sr.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px");
        grid.addColumn(sr -> sr.getStudent().getName()).setHeader("Student Name").setFlexGrow(1);
        grid.addColumn(sr -> sr.getGpa() != null ? sr.getGpa().toPlainString() : "0.00").setHeader("GPA").setWidth("100px");
        grid.addColumn(sr -> sr.getCgpa() != null ? sr.getCgpa().toPlainString() : "0.00").setHeader("CGPA").setWidth("100px");
        grid.addColumn(SemesterResult::getResultReference).setHeader("Result Reference Code").setWidth("220px");

        grid.addComponentColumn(sr -> {
            Span b = new Span(sr.getWorkflowStatus().name());
            b.getElement().getThemeList().add(sr.getWorkflowStatus() == WorkflowStatus.PUBLISHED ? "badge success small" : "badge primary small");
            return b;
        }).setHeader("Workflow Status").setWidth("160px");

        add(title, info, bar, slaIndicator, grid);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }
    }

    private void refreshView() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        grid.setItems(resultService.getSemesterResultsForExam(exam.getId()));

        slaIndicator.removeAll();
        boolean isOverdue = workflowService.isPublicationOverdue(exam.getId(), 3);
        if (isOverdue) {
            Span alert = new Span("⚠️ SLA ALERT: Publication is OVERDUE (> 3 days since RPB approval date)");
            alert.getElement().getThemeList().add("badge error");
            alert.getStyle().set("font-size", "var(--lumo-font-size-m)").set("padding", "8px 12px");
            slaIndicator.add(alert);
        } else if (rpbMeetingRepository.findByExamId(exam.getId()).isPresent()) {
            Span ok = new Span("✓ RPB Approved within 3-day publication target window");
            ok.getElement().getThemeList().add("badge success");
            slaIndicator.add(ok);
        }
    }

    private void executePublish() {
        Examination exam = examSelect.getValue();
        if (exam == null) return;

        try {
            String user = authenticatedUser.get().map(u -> u.getFullName()).orElse("Controller of Examinations");
            workflowService.publishResults(exam.getId(), user);
            Notification.show("Results successfully published to the student portal!", 4000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshView();
        } catch (Exception ex) {
            Notification.show("Publication Blocked: " + ex.getMessage(), 6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
