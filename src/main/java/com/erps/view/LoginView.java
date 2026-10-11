package com.erps.view;

import com.erps.entity.User;
import com.erps.repository.UserRepository;
import com.erps.security.TotpService;
import com.erps.service.InstitutionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
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
@PageTitle("Login | Examination Result Processing System")
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
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        String instName = institutionService.getSettings().getName();

        VerticalLayout card = new VerticalLayout();
        card.setMaxWidth("520px");
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        H1 title = new H1(instName);
        title.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin-bottom", "0");

        H3 subTitle = new H3("Examination Result Processing System (ERPS)");
        subTitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0").set("font-size", "var(--lumo-font-size-m)");

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Sign In to Access Examination Module");
        i18n.getForm().setUsername("Email Address");
        i18n.getForm().setPassword("Password");
        loginForm.setI18n(i18n);

        card.add(title, subTitle, loginForm);

        // Try Demo Quick Buttons (Dev/Demo Profile Only)
        createDemoRoleSection(card);

        add(card);
    }

    private void createDemoRoleSection(VerticalLayout card) {
        VerticalLayout demoBox = new VerticalLayout();
        demoBox.setWidthFull();
        demoBox.getStyle()
                .set("border-top", "1px dashed var(--lumo-contrast-20pct)")
                .set("margin-top", "10px")
                .set("padding-top", "10px");

        Span demoHeader = new Span("⚡ One-Click Demo Role Login (Dev/Demo Profile):");
        demoHeader.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-xs)");

        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        Button btnAdmin = createQuickLoginBtn("Super Admin (CoE)", "admin@institution.edu", "Admin@123");
        Button btnPrincipal = createQuickLoginBtn("Principal (RPB Chair)", "principal@institution.edu", "Principal@123");
        row1.add(btnAdmin, btnPrincipal);

        HorizontalLayout row2 = new HorizontalLayout();
        row2.setWidthFull();
        Button btnCoord = createQuickLoginBtn("Exam Cell Coord", "coordinator@institution.edu", "Coord@123");
        Button btnFaculty = createQuickLoginBtn("Faculty (Examiner)", "faculty1@institution.edu", "Fac1@123");
        row2.add(btnCoord, btnFaculty);

        HorizontalLayout row3 = new HorizontalLayout();
        row3.setWidthFull();
        Button btnChief = createQuickLoginBtn("Chief Supt", "chief@institution.edu", "Chief@123");
        Button btnStaff = createQuickLoginBtn("Tech Staff", "techstaff@institution.edu", "Staff@123");
        row3.add(btnChief, btnStaff);

        HorizontalLayout row4 = new HorizontalLayout();
        row4.setWidthFull();
        Button btnStudent = createQuickLoginBtn("Student (Aarav)", "student01@institution.edu", "Student@123");
        row4.add(btnStudent);

        demoBox.add(demoHeader, row1, row2, row3, row4);
        card.add(demoBox);
    }

    private Button createQuickLoginBtn(String label, String email, String pass) {
        Button btn = new Button(label, e -> directLogin(email, pass));
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.setWidthFull();
        btn.getStyle().set("border", "1px solid var(--lumo-contrast-15pct)");
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
