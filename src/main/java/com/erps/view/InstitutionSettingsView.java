package com.erps.view;

import com.erps.entity.InstitutionSettings;
import com.erps.service.InstitutionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Institution Branding & Settings | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class InstitutionSettingsView extends VerticalLayout {

    private final InstitutionService institutionService;

    public InstitutionSettingsView(InstitutionService institutionService) {
        this.institutionService = institutionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Institution Branding & Affiliation Configuration (Section 0)");
        Paragraph desc = new Paragraph("Policy Requirement: Institution name, address, and affiliating university name are strictly configurable and never hard-coded. Changes immediately propagate to all mark sheets, headers, and reports.");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        InstitutionSettings settings = institutionService.getSettings();

        TextField nameField = new TextField("Institution Name");
        nameField.setWidthFull();
        nameField.setValue(settings.getName());

        TextArea addressField = new TextArea("Campus Address & Location");
        addressField.setWidthFull();
        addressField.setValue(settings.getAddress() != null ? settings.getAddress() : "");

        TextField univField = new TextField("Affiliating University Name");
        univField.setWidthFull();
        univField.setValue(settings.getUniversityName());

        TextField logoField = new TextField("Logo Reference / Path");
        logoField.setWidthFull();
        logoField.setValue(settings.getLogoRef() != null ? settings.getLogoRef() : "logo.png");

        FormLayout form = new FormLayout(nameField, univField, addressField, logoField);
        form.setMaxWidth("700px");

        Button saveBtn = new Button("Save Configuration", VaadinIcon.CHECK.create(), e -> {
            institutionService.updateSettings(nameField.getValue(), addressField.getValue(),
                    univField.getValue(), logoField.getValue());

            Notification.show("Institution settings saved! Header and mark sheets updated.",
                    4000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().getPage().reload();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(title, desc, form, saveBtn);
    }
}
