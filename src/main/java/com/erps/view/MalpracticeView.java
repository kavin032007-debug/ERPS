package com.erps.view;

import com.erps.entity.*;
import com.erps.repository.StudentRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.MalpracticeService;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "malpractice", layout = MainLayout.class)
@PageTitle("Malpractice Incident Registry | ERPS")
@RolesAllowed({"CHIEF_SUPERINTENDENT", "SUPER_ADMIN"})
public class MalpracticeView extends VerticalLayout {

    private final MalpracticeService malpracticeService;
    private final ExaminationService examinationService;
    private final MasterDataService masterDataService;
    private final StudentRepository studentRepository;
    private final AuthenticatedUser authenticatedUser;

    private final Grid<MalpracticeCase> grid = new Grid<>(MalpracticeCase.class, false);

    public MalpracticeView(MalpracticeService malpracticeService,
                           ExaminationService examinationService,
                           MasterDataService masterDataService,
                           StudentRepository studentRepository,
                           AuthenticatedUser authenticatedUser) {
        this.malpracticeService = malpracticeService;
        this.examinationService = examinationService;
        this.masterDataService = masterDataService;
        this.studentRepository = studentRepository;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Examination Hall Malpractice Incident Register (FR9)");
        Paragraph desc = new Paragraph("Policy Rule: Chief Superintendent flags candidate during exam hall incidents. The course result is automatically FROZEN (HELD) and blocked from approval/publication until formal Discipline Committee adjudication.");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout bar = new HorizontalLayout();
        Button reportBtn = new Button("File Malpractice Incident Report", VaadinIcon.FLAG.create(), e -> openReportDialog());
        reportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        bar.add(reportBtn);

        configureGrid();
        refreshGrid();

        add(title, desc, bar, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(m -> m.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px");
        grid.addColumn(m -> m.getStudent().getName()).setHeader("Student Name").setFlexGrow(1);
        grid.addColumn(m -> m.getCourse().getCode()).setHeader("Course").setWidth("120px");
        grid.addColumn(MalpracticeCase::getReason).setHeader("Incident Report").setFlexGrow(2);
        grid.addColumn(MalpracticeCase::getFlaggedBy).setHeader("Reported By").setWidth("180px");

        grid.addComponentColumn(m -> {
            Span badge = new Span(m.getDecision().name());
            String theme = switch (m.getDecision()) {
                case PENDING -> "badge error small";
                case CLEARED -> "badge success small";
                case PENALIZED -> "badge contrast small";
            };
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader("Enquiry Decision").setWidth("150px");

        grid.addComponentColumn(this::createActionButtons).setHeader("Committee Actions").setWidth("200px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(MalpracticeCase mc) {
        HorizontalLayout layout = new HorizontalLayout();

        if (mc.getDecision() == MalpracticeDecision.PENDING && authenticatedUser.hasRole(Role.SUPER_ADMIN)) {
            Button resolveBtn = new Button("Adjudicate", VaadinIcon.GAVEL.create(), e -> openResolveDialog(mc));
            resolveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            layout.add(resolveBtn);
        } else {
            layout.add(new Span(mc.getPenaltyNotes() != null ? mc.getPenaltyNotes() : "-"));
        }

        return layout;
    }

    private void openReportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("File Malpractice Report (Chief Superintendent)");
        dialog.setWidth("600px");

        Select<Examination> examSelect = new Select<>();
        examSelect.setLabel("Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());

        Select<Student> studentSelect = new Select<>();
        studentSelect.setLabel("Candidate");
        studentSelect.setItemLabelGenerator(s -> s.getRegisterNo() + " - " + s.getName());
        studentSelect.setItems(masterDataService.getAllStudents());

        Select<Course> courseSelect = new Select<>();
        courseSelect.setLabel("Subject / Course");
        courseSelect.setItemLabelGenerator(c -> c.getCode() + " - " + c.getTitle());
        courseSelect.setItems(masterDataService.getAllCourses());

        TextArea reasonArea = new TextArea("Detailed Incident Description");
        reasonArea.setWidthFull();
        reasonArea.setPlaceholder("Detail the material confiscated, hall location, and nature of infraction.");

        dialog.add(new FormLayout(examSelect, studentSelect, courseSelect, reasonArea));

        Button confirmBtn = new Button("Submit Incident & Freeze Marks", e -> {
            try {
                String user = authenticatedUser.get().map(User::getFullName).orElse("Chief Superintendent");
                malpracticeService.flagMalpractice(examSelect.getValue(), studentSelect.getValue(),
                        courseSelect.getValue(), reasonArea.getValue(), user);

                Notification.show("Malpractice registered! Course result has been automatically FROZEN (HELD).",
                        5000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void openResolveDialog(MalpracticeCase mc) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Discipline Committee Decision: " + mc.getStudent().getRegisterNo());
        dialog.setWidth("550px");

        Select<MalpracticeDecision> decisionSelect = new Select<>();
        decisionSelect.setLabel("Committee Decision");
        decisionSelect.setItems(MalpracticeDecision.CLEARED, MalpracticeDecision.PENALIZED);
        decisionSelect.setValue(MalpracticeDecision.CLEARED);

        TextArea notesArea = new TextArea("Committee Minutes & Penalty Notes");
        notesArea.setWidthFull();
        notesArea.setValue("Disciplinary Enquiry Committee reviewed answer script and found no malicious material.");

        dialog.add(new FormLayout(decisionSelect, notesArea));

        Button confirmBtn = new Button("Record Decision & Update Marks", e -> {
            try {
                String decider = authenticatedUser.get().map(User::getFullName).orElse("Controller of Examinations");
                malpracticeService.resolveCase(mc.getId(), decisionSelect.getValue(), notesArea.getValue(), decider);

                Notification.show("Decision recorded. Result status updated accordingly.",
                        4000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(malpracticeService.getAllCases());
    }
}
