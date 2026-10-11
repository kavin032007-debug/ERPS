package com.erps;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Push
public class ErpsApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(ErpsApplication.class, args);
    }
}
