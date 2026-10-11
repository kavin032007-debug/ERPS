package com.erps.view;

import com.erps.audit.AuditService;
import com.erps.entity.*;
import com.erps.repository.*;
import com.erps.security.AuthenticatedUser;
import com.erps.service.AnalyticsService;
import com.erps.service.ExaminationService;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.router.RouteAlias;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard | ERPS")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(AuthenticatedUser authenticatedUser,
                         AnalyticsService analyticsService,
                         AuditService auditService,
                         ExaminationService examinationService,
                         MasterDataService masterDataService,
                         StudentRepository studentRepository,
                         SemesterResultRepository semesterResultRepository,
                         MarkAlterationRepository markAlterationRepository,
                         MalpracticeCaseRepository malpracticeCaseRepository,
                         RevaluationRequestRepository revaluationRequestRepository) {

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Optional<User> userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            add(new Paragraph("Please log in to continue."));
            return;
        }

        User user = userOpt.get();
        Role role = user.getRole();

        // Welcome Header Card
        VerticalLayout welcomeCard = new VerticalLayout();
        welcomeCard.setWidthFull();
        welcomeCard.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color-10pct), var(--lumo-primary-color-5pct))")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "20px")
                .set("border", "1px solid var(--lumo-primary-color-20pct)");

        H2 greeting = new H2("Welcome, " + user.getFullName() + "!");
        greeting.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-xl)");

        Paragraph roleDesc = new Paragraph("Active Role: " + role.name().replace("_", " ") + " | Examination Session: 2025-2026 Academic Year");
        roleDesc.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

        welcomeCard.add(greeting, roleDesc);
        add(welcomeCard);

        if (role == Role.STUDENT) {
            buildStudentDashboard(user, studentRepository, semesterResultRepository);
        } else {
            buildStaffDashboard(role, analyticsService, auditService, markAlterationRepository,
                    malpracticeCaseRepository, revaluationRequestRepository);
        }
    }

    private void buildStudentDashboard(User user, StudentRepository studentRepository, SemesterResultRepository semesterResultRepository) {
        Optional<Student> studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isEmpty()) {
            add(new Paragraph("No student record linked to account."));
            return;
        }

        Student student = studentOpt.get();
        List<SemesterResult> publishedResults = semesterResultRepository.findByStudentIdAndWorkflowStatus(student.getId(), WorkflowStatus.PUBLISHED);

        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setWidthFull();

        statsRow.add(createCard("Register Number", student.getRegisterNo(), VaadinIcon.USER.create(), "primary"));
        statsRow.add(createCard("Department & Programme", student.getProgramme().getCode(), VaadinIcon.BUILDING.create(), "success"));
        statsRow.add(createCard("Current Semester", "Semester " + student.getCurrentSemester(), VaadinIcon.CALENDAR.create(), "contrast"));

        if (!publishedResults.isEmpty()) {
            SemesterResult latest = publishedResults.get(publishedResults.size() - 1);
            statsRow.add(createCard("Latest GPA", latest.getGpa() != null ? latest.getGpa().toPlainString() : "0.00", VaadinIcon.TROPHY.create(), "primary"));
            statsRow.add(createCard("CGPA", latest.getCgpa() != null ? latest.getCgpa().toPlainString() : "0.00", VaadinIcon.STAR.create(), "success"));
            statsRow.add(createCard("Arrear Count", String.valueOf(latest.getArrearCount()), VaadinIcon.WARNING.create(), latest.isPass() ? "success" : "error"));
        }

        add(statsRow);

        VerticalLayout actions = new VerticalLayout();
        actions.getStyle().set("margin-top", "20px");
        H3 actionTitle = new H3("Student Quick Actions");
        Button viewResultsBtn = new Button("View Published Grade Sheets & Download PDF", VaadinIcon.FILE_TEXT.create(), e -> UI.getCurrent().navigate(StudentPortalView.class));
        viewResultsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actions.add(actionTitle, viewResultsBtn);
        add(actions);
    }

    private void buildStaffDashboard(Role role, AnalyticsService analyticsService, AuditService auditService,
                                     MarkAlterationRepository markAlterationRepository,
                                     MalpracticeCaseRepository malpracticeCaseRepository,
                                     RevaluationRequestRepository revaluationRequestRepository) {
        AnalyticsService.DashboardOverview overview = analyticsService.getDashboardOverview(null);

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();
        kpiRow.add(createCard("Total Students", String.valueOf(overview.totalStudents()), VaadinIcon.USERS.create(), "primary"));
        kpiRow.add(createCard("Published Results", String.valueOf(overview.totalPublished()), VaadinIcon.CHECK_CIRCLE.create(), "success"));
        kpiRow.add(createCard("Pass Percentage", overview.passPercentage() + "%", VaadinIcon.DASHBOARD.create(), "primary"));
        kpiRow.add(createCard("Arrears / Fails", String.valueOf(overview.failedCount()), VaadinIcon.EXCLAMATION_CIRCLE.create(), "error"));

        long pendingAlterations = markAlterationRepository.countByStatus(AlterationStatus.PENDING);
        long pendingMalpractice = malpracticeCaseRepository.count();
        long pendingRevals = revaluationRequestRepository.count();

        kpiRow.add(createCard("Pending Alterations", String.valueOf(pendingAlterations), VaadinIcon.BELL.create(), pendingAlterations > 0 ? "error" : "success"));

        add(kpiRow);

        // Quick Navigation Grid
        H3 navTitle = new H3("Quick Navigation & Workflow Shortcuts");
        HorizontalLayout shortcuts = new HorizontalLayout();
        shortcuts.setWidthFull();

        if (role == Role.SUPER_ADMIN) {
            shortcuts.add(createQuickButton("Calculate Results", ResultCalculationView.class, VaadinIcon.CALC_BOOK));
            shortcuts.add(createQuickButton("Moderation (Grace)", ModerationView.class, VaadinIcon.SLIDERS));
            shortcuts.add(createQuickButton("RPB Approval", RpbApprovalView.class, VaadinIcon.CHECK_SQUARE));
            shortcuts.add(createQuickButton("Publish Results", PublicationView.class, VaadinIcon.GLOBE));
            shortcuts.add(createQuickButton("Audit Hash Chain", AuditLogView.class, VaadinIcon.SHIELD));
        } else if (role == Role.FACULTY) {
            shortcuts.add(createQuickButton("CIA Marks Entry", CiaMarksEntryView.class, VaadinIcon.EDIT));
            shortcuts.add(createQuickButton("SEE Marks (Dummy ID)", SeeMarksEntryView.class, VaadinIcon.SHIELD));
        } else if (role == Role.PRINCIPAL) {
            shortcuts.add(createQuickButton("RPB Board Approval", RpbApprovalView.class, VaadinIcon.CHECK_CIRCLE));
            shortcuts.add(createQuickButton("Analytics & Reports", AnalyticsView.class, VaadinIcon.CHART));
        } else if (role == Role.TECH_STAFF) {
            shortcuts.add(createQuickButton("Revaluation Desk", RevaluationDeskView.class, VaadinIcon.REFRESH));
            shortcuts.add(createQuickButton("Student Management", StudentManagementView.class, VaadinIcon.USERS));
        } else if (role == Role.CHIEF_SUPERINTENDENT) {
            shortcuts.add(createQuickButton("Malpractice Reporting", MalpracticeView.class, VaadinIcon.WARNING));
        }

        add(navTitle, shortcuts);
    }

    private Button createQuickButton(String title, Class<? extends com.vaadin.flow.component.Component> target, VaadinIcon icon) {
        Button btn = new Button(title, icon.create(), e -> UI.getCurrent().navigate(target));
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.getStyle().set("padding", "15px 20px");
        return btn;
    }

    private VerticalLayout createCard(String title, String value, com.vaadin.flow.component.Component icon, String theme) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)").set("font-weight", "500");
        top.add(titleSpan, icon);

        H2 valHeading = new H2(value);
        valHeading.getStyle().set("margin", "8px 0 0 0").set("font-size", "var(--lumo-font-size-xxl)");

        card.add(top, valHeading);
        return card;
    }
}
