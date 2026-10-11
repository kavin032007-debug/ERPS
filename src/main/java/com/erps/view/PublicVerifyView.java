package com.erps.view;

import com.erps.service.InstitutionService;
import com.erps.service.MarksheetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Map;

@Route("verify")
@PageTitle("Public Mark Sheet Verification | ERPS")
@AnonymousAllowed
public class PublicVerifyView extends VerticalLayout implements BeforeEnterObserver {

    private final MarksheetService marksheetService;
    private final InstitutionService institutionService;

    private final TextField refInput = new TextField();
    private final VerticalLayout resultCard = new VerticalLayout();

    public PublicVerifyView(MarksheetService marksheetService, InstitutionService institutionService) {
        this.marksheetService = marksheetService;
        this.institutionService = institutionService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        VerticalLayout card = new VerticalLayout();
        card.setMaxWidth("600px");
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        H2 header = new H2("Official Document Verification Portal");
        header.getStyle().set("margin-bottom", "0");

        Paragraph sub = new Paragraph(institutionService.getSettings().getName() + " - Autonomous Examination System");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        Paragraph privacyNotice = new Paragraph("PRIVACY NOTICE: In compliance with institutional confidentiality protocols, this portal validates authentic documents without disclosing grades, marks, or personal identification.");
        privacyNotice.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-tertiary-text-color)");

        refInput.setLabel("Result Reference Code (from Mark Sheet QR Code)");
        refInput.setWidthFull();
        refInput.setPlaceholder("e.g. ERPS-1-24CS001-XXXX");

        Button verifyBtn = new Button("Verify Authenticity", VaadinIcon.SEARCH.create(), e -> doVerification(refInput.getValue()));
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyBtn.setWidthFull();

        resultCard.setWidthFull();
        resultCard.setVisible(false);

        Anchor loginLink = new Anchor("login", "Back to System Sign-In");
        loginLink.getStyle().set("font-size", "var(--lumo-font-size-s)").set("margin-top", "10px");

        card.add(header, sub, privacyNotice, refInput, verifyBtn, resultCard, loginLink);
        add(card);
    }

    private void doVerification(String ref) {
        resultCard.removeAll();
        resultCard.setVisible(true);

        MarksheetService.VerificationSummary res = marksheetService.verifyResultReference(ref);

        resultCard.getStyle()
                .set("border-radius", "8px")
                .set("padding", "15px");

        if (res.valid()) {
            resultCard.getStyle()
                    .set("background", "var(--lumo-success-color-10pct)")
                    .set("border", "1px solid var(--lumo-success-color-50pct)");

            H3 validHeading = new H3("✓ AUTHENTIC MARK SHEET");
            validHeading.getStyle().set("color", "var(--lumo-success-text-color)").set("margin", "0 0 10px 0");

            Paragraph p1 = new Paragraph("Result Reference: " + res.resultReference());
            Paragraph p2 = new Paragraph("Candidate Initials: " + res.studentInitials() + " (Full name masked for privacy)");
            Paragraph p3 = new Paragraph("Examination Session: " + res.examName());
            Paragraph p4 = new Paragraph("Publication Date: " + res.publishedDate());

            resultCard.add(validHeading, p1, p2, p3, p4);
        } else {
            resultCard.getStyle()
                    .set("background", "var(--lumo-error-color-10pct)")
                    .set("border", "1px solid var(--lumo-error-color-50pct)");

            H3 invalidHeading = new H3("✕ INVALID OR UNVERIFIED REFERENCE");
            invalidHeading.getStyle().set("color", "var(--lumo-error-text-color)").set("margin", "0 0 10px 0");

            Paragraph info = new Paragraph("No published examination result matches the specified reference code. This document cannot be verified as authentic.");
            resultCard.add(invalidHeading, info);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        if (params.containsKey("ref") && !params.get("ref").isEmpty()) {
            String ref = params.get("ref").get(0);
            refInput.setValue(ref);
            doVerification(ref);
        }
    }
}
