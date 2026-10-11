package com.erps.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("access-denied")
@PageTitle("Access Denied (403) | ERPS")
@AnonymousAllowed
public class AccessDeniedView extends VerticalLayout {

    public AccessDeniedView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 code = new H1("403 - Access Denied");
        code.getStyle().set("color", "var(--lumo-error-text-color)");

        Paragraph msg = new Paragraph("You do not have the required role or authorization to access this examination module.");
        Button homeBtn = new Button("Return to Dashboard", VaadinIcon.HOME.create(), e -> UI.getCurrent().navigate(DashboardView.class));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(code, msg, homeBtn);
    }
}
