package com.erps.view;

import com.erps.entity.Role;
import com.erps.entity.User;
import com.erps.security.AuthenticatedUser;
import com.erps.service.InstitutionService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import java.util.Optional;

/**
 * Main application layout providing a responsive header, sidebar navigation with RBAC filtering,
 * user status badge, and institution branding.
 */
public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;
    private final InstitutionService institutionService;

    public MainLayout(AuthenticatedUser authenticatedUser, InstitutionService institutionService) {
        this.authenticatedUser = authenticatedUser;
        this.institutionService = institutionService;

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        String instName = institutionService.getSettings().getName();
        H2 title = new H2(instName + " - Examination Result Processing System");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)");

        Span demoBadge = new Span("LIVE WEB EDITION");
        demoBadge.getElement().getThemeList().add("badge success small");
        demoBadge.getStyle().set("margin-left", "8px");

        HorizontalLayout titleLayout = new HorizontalLayout(title, demoBadge);
        titleLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Optional<User> userOpt = authenticatedUser.get();
        HorizontalLayout userLayout = new HorizontalLayout();
        userLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        userLayout.setSpacing(true);

        if (userOpt.isPresent()) {
            User u = userOpt.get();
            Span nameSpan = new Span(u.getFullName());
            nameSpan.getStyle().set("font-weight", "500").set("font-size", "var(--lumo-font-size-s)");

            Span roleBadge = new Span(u.getRole().name().replace("_", " "));
            roleBadge.getElement().getThemeList().add("badge primary small");

            Button logoutBtn = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> authenticatedUser.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            userLayout.add(nameSpan, roleBadge, logoutBtn);
        }

        HorizontalLayout header = new HorizontalLayout(toggle, titleLayout, userLayout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(titleLayout);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        header.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-base-color)");

        addToNavbar(true, header);
    }

    private void createDrawer() {
        Optional<User> userOpt = authenticatedUser.get();
        Role userRole = userOpt.map(User::getRole).orElse(Role.STUDENT);

        SideNav nav = new SideNav();
        nav.setWidthFull();

        // 1. Overview
        SideNavItem dashboardItem = new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create());
        nav.addItem(dashboardItem);

        if (userRole != Role.STUDENT) {
            SideNavItem analyticsItem = new SideNavItem("Analytics & Reports", AnalyticsView.class, VaadinIcon.CHART.create());
            nav.addItem(analyticsItem);
        }

        // 2. Student Portal (Student only or Admin view)
        if (userRole == Role.STUDENT || userRole == Role.SUPER_ADMIN) {
            SideNavItem studentPortalItem = new SideNavItem("My Published Results", StudentPortalView.class, VaadinIcon.ACADEMY_CAP.create());
            nav.addItem(studentPortalItem);
        }

        // 3. Master Data
        if (userRole == Role.SUPER_ADMIN || userRole == Role.TECH_STAFF || userRole == Role.EXAM_CELL_COORDINATOR) {
            SideNavItem masterGroup = new SideNavItem("Master Data");
            masterGroup.setPrefixComponent(VaadinIcon.DATABASE.create());
            masterGroup.addItem(new SideNavItem("Departments & Programmes", DepartmentProgrammeView.class, VaadinIcon.BUILDING.create()));
            masterGroup.addItem(new SideNavItem("Courses & Semesters", CourseView.class, VaadinIcon.BOOK.create()));
            masterGroup.addItem(new SideNavItem("Students & Registrations", StudentManagementView.class, VaadinIcon.USERS.create()));
            if (userRole == Role.SUPER_ADMIN) {
                masterGroup.addItem(new SideNavItem("Faculty & Users", UserManagementView.class, VaadinIcon.USER_CHECK.create()));
            }
            nav.addItem(masterGroup);
        }

        // 4. Examination & Marks Entry
        if (userRole != Role.STUDENT) {
            SideNavItem examGroup = new SideNavItem("Examinations & Entry");
            examGroup.setPrefixComponent(VaadinIcon.CALENDAR_CLOCK.create());

            if (userRole == Role.SUPER_ADMIN || userRole == Role.EXAM_CELL_COORDINATOR || userRole == Role.TECH_STAFF) {
                examGroup.addItem(new SideNavItem("Examinations", ExaminationManagementView.class, VaadinIcon.CLIPBOARD_CHECK.create()));
            }

            if (userRole == Role.FACULTY || userRole == Role.EXAM_CELL_COORDINATOR || userRole == Role.SUPER_ADMIN) {
                examGroup.addItem(new SideNavItem("CIA Marks Entry", CiaMarksEntryView.class, VaadinIcon.EDIT.create()));
            }

            if (userRole == Role.FACULTY || userRole == Role.SUPER_ADMIN) {
                examGroup.addItem(new SideNavItem("SEE Marks Entry (Dummy IDs)", SeeMarksEntryView.class, VaadinIcon.SHIELD.create()));
            }
            nav.addItem(examGroup);
        }

        // 5. Processing & RPB Governance
        if (userRole == Role.SUPER_ADMIN || userRole == Role.PRINCIPAL) {
            SideNavItem processGroup = new SideNavItem("Result Processing & RPB");
            processGroup.setPrefixComponent(VaadinIcon.COGS.create());

            if (userRole == Role.SUPER_ADMIN) {
                processGroup.addItem(new SideNavItem("Result Calculation", ResultCalculationView.class, VaadinIcon.CALC_BOOK.create()));
                processGroup.addItem(new SideNavItem("Moderation (Grace Marks)", ModerationView.class, VaadinIcon.SLIDERS.create()));
            }

            processGroup.addItem(new SideNavItem("RPB Approval (Board)", RpbApprovalView.class, VaadinIcon.CHECK_CIRCLE.create()));

            if (userRole == Role.SUPER_ADMIN) {
                processGroup.addItem(new SideNavItem("Result Publication", PublicationView.class, VaadinIcon.GLOBE.create()));
                processGroup.addItem(new SideNavItem("Mark Alteration Queue", MarkAlterationView.class, VaadinIcon.EXCLAMATION_CIRCLE.create()));
            }
            nav.addItem(processGroup);
        }

        // 6. Discipline & Revaluation
        if (userRole != Role.STUDENT) {
            SideNavItem disciplineGroup = new SideNavItem("Discipline & Reval");
            disciplineGroup.setPrefixComponent(VaadinIcon.GAVEL.create());

            if (userRole == Role.CHIEF_SUPERINTENDENT || userRole == Role.SUPER_ADMIN) {
                disciplineGroup.addItem(new SideNavItem("Malpractice Cases", MalpracticeView.class, VaadinIcon.WARNING.create()));
            }

            if (userRole == Role.TECH_STAFF || userRole == Role.SUPER_ADMIN) {
                disciplineGroup.addItem(new SideNavItem("Revaluation Desk", RevaluationDeskView.class, VaadinIcon.REFRESH.create()));
            }
            nav.addItem(disciplineGroup);
        }

        // 7. Configuration & System
        if (userRole == Role.SUPER_ADMIN) {
            SideNavItem configGroup = new SideNavItem("Configuration & Logs");
            configGroup.setPrefixComponent(VaadinIcon.WRENCH.create());
            configGroup.addItem(new SideNavItem("Grading Regulations", GradingConfigView.class, VaadinIcon.OPTIONS.create()));
            configGroup.addItem(new SideNavItem("Institution Settings", InstitutionSettingsView.class, VaadinIcon.INSTITUTION.create()));
            configGroup.addItem(new SideNavItem("Audit Log (Hash Chain)", AuditLogView.class, VaadinIcon.FILE_TEXT.create()));
            nav.addItem(configGroup);
        }

        // 8. Account & Security
        SideNavItem accountItem = new SideNavItem("Account & MFA Security", AccountView.class, VaadinIcon.KEY.create());
        nav.addItem(accountItem);

        VerticalLayout drawerLayout = new VerticalLayout(nav);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);

        addToDrawer(drawerLayout);
    }
}
