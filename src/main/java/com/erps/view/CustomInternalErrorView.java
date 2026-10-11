package com.erps.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletResponse;

@AnonymousAllowed
@PageTitle("System Notice (500) | ERPS")
public class CustomInternalErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    public CustomInternalErrorView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 code = new H1("500 - Unexpected System Condition");
        code.getStyle().set("color", "var(--lumo-error-text-color)");

        Paragraph msg = new Paragraph("An unexpected condition occurred while processing this request. Stack traces are masked for security. Please contact the Examination Office or try again.");
        Button homeBtn = new Button("Return to Dashboard", VaadinIcon.HOME.create(), e -> UI.getCurrent().navigate(DashboardView.class));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(code, msg, homeBtn);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
