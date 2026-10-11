package com.erps.view;

import com.erps.entity.User;
import com.erps.repository.UserRepository;
import com.erps.security.TotpService;
import com.erps.service.InstitutionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

@Route("login")
@PageTitle("Sign In | Examination Result Processing System")
@AnonymousAllowed
@Uses(Icon.class)
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();
    private final InstitutionService institutionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    @Value("${app.mfa.enabled:false}")
    private boolean mfaConfigEnabled;

    @Value("${app.demo.seed-data:true}")
    private boolean demoButtonsEnabled;

    public LoginView(InstitutionService institutionService,
                     UserRepository userRepository,
                     PasswordEncoder passwordEncoder,
                     TotpService totpService) {
        this.institutionService = institutionService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setPadding(true);
        setSpacing(false);
        getStyle()
                .set("background", "radial-gradient(ellipse at top, #1e293b, #0f172a)")
                .set("min-height", "100vh")
                .set("box-sizing", "border-box")
                .set("overflow-y", "auto");

        String instName = institutionService.getSettings().getName();

        // Card Wrapper with clean elevation & border
        VerticalLayout card = new VerticalLayout();
        card.setMaxWidth("460px");
        card.setWidthFull();
        card.setPadding(false);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "16px")
                .set("box-shadow", "0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1)")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden");

        // Header Section
        VerticalLayout header = new VerticalLayout();
        header.setPadding(true);
        header.setSpacing(false);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(180deg, #f8fafc 0%, #ffffff 100%)")
                .set("border-bottom", "1px solid #f1f5f9")
                .set("padding", "24px 20px 16px 20px")
                .set("box-sizing", "border-box");

        // Institution Icon Badge
        Div iconBadge = new Div();
        Icon capIcon = VaadinIcon.ACADEMY_CAP.create();
        capIcon.setSize("26px");
        capIcon.setColor("#2563eb");
        iconBadge.add(capIcon);
        iconBadge.getStyle()
                .set("width", "52px")
                .set("height", "52px")
                .set("border-radius", "14px")
                .set("background", "#eff6ff")
                .set("border", "1px solid #dbeafe")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("margin-bottom", "10px");

        H2 title = new H2(instName);
        title.getStyle()
                .set("font-size", "1.25rem")
                .set("font-weight", "700")
                .set("color", "#0f172a")
                .set("margin", "0 0 4px 0")
                .set("text-align", "center");

        Paragraph subTitle = new Paragraph("Examination Result Processing System (ERPS)");
        subTitle.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.85rem")
                .set("margin", "0 0 8px 0")
                .set("text-align", "center");

        Span editionBadge = new Span("Autonomous College • Absolute Grading");
        editionBadge.getStyle()
                .set("background", "#f1f5f9")
                .set("color", "#475569")
                .set("font-size", "0.72rem")
                .set("font-weight", "600")
                .set("padding", "3px 10px")
                .set("border-radius", "20px");

        header.add(iconBadge, title, subTitle, editionBadge);

        // Form Body
        VerticalLayout body = new VerticalLayout();
        body.setPadding(true);
        body.setSpacing(true);
        body.getStyle()
                .set("padding", "20px 24px 24px 24px")
                .set("box-sizing", "border-box");

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.getStyle()
                .set("width", "100%")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Sign In to Portal");
        i18n.getForm().setUsername("Email Address");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Sign In");
        loginForm.setI18n(i18n);

        body.add(loginForm);

        // One-Click Demo Role Section with Clean 2-Column CSS Grid
        createDemoRoleSection(body);

        card.add(header, body);
        add(card);
    }

    private void createDemoRoleSection(VerticalLayout container) {
        VerticalLayout demoBox = new VerticalLayout();
        demoBox.setWidthFull();
        demoBox.setPadding(false);
        demoBox.setSpacing(false);
        demoBox.getStyle()
                .set("border-top", "1px dashed #e2e8f0")
                .set("margin-top", "16px")
                .set("padding-top", "14px")
                .set("box-sizing", "border-box");

        Div headerRow = new Div();
        headerRow.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "6px")
                .set("margin-bottom", "10px");

        Span lightning = new Span("⚡");
        lightning.getStyle().set("font-size", "13px");

        Span demoHeader = new Span("One-Click Demo Access");
        demoHeader.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.78rem")
                .set("color", "#475569")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.03em");

        headerRow.add(lightning, demoHeader);

        // Strict 2-column grid container so items NEVER overflow
        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, minmax(0, 1fr))")
                .set("gap", "8px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        grid.add(createQuickLoginBtn("Super Admin (CoE)", "admin@institution.edu", "Admin@123", VaadinIcon.SHIELD));
        grid.add(createQuickLoginBtn("Principal (RPB Chair)", "principal@institution.edu", "Principal@123", VaadinIcon.DIPLOMA));
        grid.add(createQuickLoginBtn("Exam Cell Coord", "coordinator@institution.edu", "Coord@123", VaadinIcon.TASKS));
        grid.add(createQuickLoginBtn("Faculty Examiner", "faculty1@institution.edu", "Fac1@123", VaadinIcon.ACADEMY_CAP));
        grid.add(createQuickLoginBtn("Chief Supt", "chief@institution.edu", "Chief@123", VaadinIcon.INSTITUTION));
        grid.add(createQuickLoginBtn("Tech Staff", "techstaff@institution.edu", "Staff@123", VaadinIcon.TOOLS));

        // Full-width student button
        Button btnStudent = createQuickLoginBtn("Student Portal (Aarav)", "student01@institution.edu", "Student@123", VaadinIcon.USER);
        btnStudent.getStyle()
                .set("grid-column", "1 / -1")
                .set("background", "#eff6ff")
                .set("border-color", "#bfdbfe")
                .set("color", "#1d4ed8");
        grid.add(btnStudent);

        // Public QR Verification Button (No login needed)
        Button btnVerify = new Button("Public Mark Sheet QR Verifier", VaadinIcon.QRCODE.create(), e -> UI.getCurrent().navigate(PublicVerifyView.class));
        btnVerify.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btnVerify.getStyle()
                .set("grid-column", "1 / -1")
                .set("width", "100%")
                .set("margin-top", "4px")
                .set("border", "1px dashed #cbd5e1")
                .set("color", "#64748b")
                .set("font-size", "11px");
        grid.add(btnVerify);

        demoBox.add(headerRow, grid);
        container.add(demoBox);
    }

    private Button createQuickLoginBtn(String label, String email, String pass, VaadinIcon icon) {
        Button btn = new Button(label, icon.create(), e -> directLogin(email, pass));
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btn.getStyle()
                .set("width", "100%")
                .set("min-width", "0")
                .set("border", "1px solid #e2e8f0")
                .set("background", "#f8fafc")
                .set("border-radius", "8px")
                .set("font-size", "11px")
                .set("font-weight", "500")
                .set("color", "#334155")
                .set("padding", "6px 8px")
                .set("box-sizing", "border-box")
                .set("cursor", "pointer")
                .set("text-align", "center")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");
        return btn;
    }

    private void directLogin(String email, String rawPass) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPass, user.getPasswordHash())) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(
                                user.getEmail(), user.getPasswordHash(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        ),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                UI.getCurrent().navigate(DashboardView.class);
            } else {
                Notification.show("Invalid credentials", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
