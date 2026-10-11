package com.erps.view;

import com.erps.audit.AuditService;
import com.erps.entity.User;
import com.erps.repository.UserRepository;
import com.erps.security.AuthenticatedUser;
import com.erps.security.TotpService;
import com.erps.service.InstitutionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route(value = "account", layout = MainLayout.class)
@PageTitle("My Account & MFA Security | ERPS")
@PermitAll
public class AccountView extends VerticalLayout {

    private final AuthenticatedUser authenticatedUser;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final InstitutionService institutionService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountView(AuthenticatedUser authenticatedUser,
                       UserRepository userRepository,
                       TotpService totpService,
                       InstitutionService institutionService,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.authenticatedUser = authenticatedUser;
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.institutionService = institutionService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        User user = authenticatedUser.get().orElse(null);
        if (user == null) {
            add(new Paragraph("User session expired."));
            return;
        }

        H2 title = new H2("Account Profile & Multi-Factor Authentication (MFA)");

        HorizontalLayout content = new HorizontalLayout();
        content.setWidthFull();
        content.setSpacing(true);

        // Left Box: Profile & Password
        VerticalLayout profileBox = new VerticalLayout();
        profileBox.setWidth("50%");
        profileBox.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "20px");

        H3 pTitle = new H3("Profile Information");
        TextField nameField = new TextField("Full Name", user.getFullName(), "");
        nameField.setReadOnly(true);
        TextField emailField = new TextField("Email Address", user.getEmail(), "");
        emailField.setReadOnly(true);
        TextField roleField = new TextField("Assigned Role", user.getRole().name(), "");
        roleField.setReadOnly(true);

        H4 passTitle = new H4("Change Password");
        PasswordField newPass = new PasswordField("New Password");
        Button changePassBtn = new Button("Update Password", e -> {
            if (newPass.getValue().length() < 6) {
                Notification.show("Password must be at least 6 characters.", 3000, Notification.Position.MIDDLE);
                return;
            }
            user.setPasswordHash(passwordEncoder.encode(newPass.getValue()));
            userRepository.save(user);
            auditService.logAction("CHANGE_PASSWORD", "User", user.getId().toString(), null, "SUCCESS");
            Notification.show("Password updated successfully.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            newPass.clear();
        });
        changePassBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        profileBox.add(pTitle, new FormLayout(nameField, emailField, roleField), passTitle, newPass, changePassBtn);

        // Right Box: TOTP MFA Setup
        VerticalLayout mfaBox = new VerticalLayout();
        mfaBox.setWidth("50%");
        mfaBox.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "20px");

        H3 mfaTitle = new H3("Two-Factor Authentication (TOTP)");
        Paragraph mfaDesc = new Paragraph("Mandatory in Production for administrative and evaluation roles. Compatible with Google Authenticator, Microsoft Authenticator, and standard TOTP apps.");
        mfaDesc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span mfaStatusBadge = new Span(Boolean.TRUE.equals(user.getMfaEnabled()) ? "MFA Active & Enforced" : "MFA Disabled");
        mfaStatusBadge.getElement().getThemeList().add(Boolean.TRUE.equals(user.getMfaEnabled()) ? "badge success" : "badge contrast");

        VerticalLayout setupPanel = new VerticalLayout();
        setupPanel.setPadding(false);

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            String secret = user.getMfaSecret() != null ? user.getMfaSecret() : totpService.generateSecret();
            user.setMfaSecret(secret);
            userRepository.save(user);

            String qrUri = totpService.getQrDataUri(secret, user.getEmail(), institutionService.getSettings().getName());
            if (qrUri != null) {
                Image qrImg = new Image(qrUri, "MFA QR Code");
                qrImg.setWidth("160px");
                qrImg.setHeight("160px");
                setupPanel.add(new Paragraph("1. Scan QR code in Authenticator App:"), qrImg);
            }

            Paragraph keyText = new Paragraph("Or manually enter secret key: " + secret);
            keyText.getStyle().set("font-family", "monospace").set("font-size", "var(--lumo-font-size-s)");

            TextField codeInput = new TextField("2. Enter 6-Digit Verification Code");
            codeInput.setWidth("200px");

            Button verifyEnableBtn = new Button("Verify & Enable MFA", VaadinIcon.SHIELD.create(), e -> {
                boolean valid = totpService.verifyCode(secret, codeInput.getValue());
                if (valid) {
                    user.setMfaEnabled(true);
                    userRepository.save(user);
                    auditService.logAction("ENABLE_MFA", "User", user.getId().toString(), "DISABLED", "ENABLED");
                    Notification.show("Two-Factor Authentication enabled successfully!", 4000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    UI.getCurrent().getPage().reload();
                } else {
                    Notification.show("Invalid verification code. Please check your authenticator clock.", 4000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            verifyEnableBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

            setupPanel.add(keyText, codeInput, verifyEnableBtn);
        } else {
            Button disableBtn = new Button("Disable MFA", VaadinIcon.CLOSE.create(), e -> {
                user.setMfaEnabled(false);
                userRepository.save(user);
                auditService.logAction("DISABLE_MFA", "User", user.getId().toString(), "ENABLED", "DISABLED");
                Notification.show("MFA disabled.");
                UI.getCurrent().getPage().reload();
            });
            disableBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            setupPanel.add(new Paragraph("MFA is currently safeguarding your account. Code will be required on each sign-in."), disableBtn);
        }

        mfaBox.add(mfaTitle, mfaDesc, mfaStatusBadge, setupPanel);

        content.add(profileBox, mfaBox);
        add(title, content);
    }
}
