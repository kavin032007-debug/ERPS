package com.erps.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletResponse;

@AnonymousAllowed
@PageTitle("Page Not Found (404) | ERPS")
public class CustomNotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public CustomNotFoundView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 code = new H1("404 - Page Not Found");
        code.getStyle().set("color", "var(--lumo-error-text-color)");

        Paragraph msg = new Paragraph("The examination module or page requested does not exist or has been relocated.");
        Button homeBtn = new Button("Return to Dashboard", VaadinIcon.HOME.create(), e -> UI.getCurrent().navigate(DashboardView.class));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(code, msg, homeBtn);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
