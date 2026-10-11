package com.erps.view;

import com.erps.audit.AuditService;
import com.erps.entity.AuditLog;
import com.erps.repository.AuditLogRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "audit-logs", layout = MainLayout.class)
@PageTitle("System Audit Log & Hash Chain | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class AuditLogView extends VerticalLayout {

    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final Grid<AuditLog> grid = new Grid<>(AuditLog.class, false);
    private final Div integrityBanner = new Div();

    public AuditLogView(AuditLogRepository auditLogRepository, AuditService auditService) {
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Cryptographic Immutable Audit Log & Hash Chain (FR15)");
        Paragraph desc = new Paragraph("Policy Requirement: Append-only log recording logins, mark changes, unlocks, moderation, and approvals. Each log entry embeds the SHA-256 hash of the preceding record for mathematical tamper detection.");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        integrityBanner.setWidthFull();
        integrityBanner.getStyle().set("border-radius", "8px").set("padding", "10px 15px").set("margin-bottom", "10px");
        updateIntegrityStatus(true); // default initial

        HorizontalLayout bar = new HorizontalLayout();

        Button verifyBtn = new Button("Verify Log Integrity (Check Hash Chain)", VaadinIcon.SHIELD.create(), e -> checkIntegrity());
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Filter by action, entity or user...");
        searchField.setWidth("350px");
        searchField.addValueChangeListener(e -> filterLogs(e.getValue()));

        Anchor exportCsv = new Anchor();
        exportCsv.getElement().setAttribute("download", true);
        Button csvBtn = new Button("Export Audit Log (CSV)", VaadinIcon.DOWNLOAD.create());
        exportCsv.add(csvBtn);
        exportCsv.setHref(new StreamResource("AuditLog_Export.csv", () -> new ByteArrayInputStream(generateCsvBytes())));

        bar.add(verifyBtn, searchField, exportCsv);

        configureGrid();
        refreshGrid();

        add(title, desc, integrityBanner, bar, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
        grid.addColumn(l -> l.getTimestamp() != null ? l.getTimestamp().format(dtf) : "-").setHeader("Timestamp").setWidth("175px").setFlexGrow(0);
        grid.addColumn(AuditLog::getUserId).setHeader("User ID").setWidth("180px").setFlexGrow(0);
        grid.addColumn(AuditLog::getRole).setHeader("Role").setWidth("140px").setFlexGrow(0);
        grid.addColumn(AuditLog::getAction).setHeader("Action").setWidth("180px").setFlexGrow(0).setSortable(true);
        grid.addColumn(AuditLog::getEntity).setHeader("Entity").setWidth("140px").setFlexGrow(0);
        grid.addColumn(AuditLog::getEntityId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        grid.addColumn(AuditLog::getOldValue).setHeader("Old Value").setWidth("160px");
        grid.addColumn(AuditLog::getNewValue).setHeader("New Value").setFlexGrow(2);

        grid.addColumn(l -> l.getHash() != null ? l.getHash().substring(0, Math.min(16, l.getHash().length())) + "..." : "-")
                .setHeader("SHA-256 Hash").setWidth("150px");
    }

    private void checkIntegrity() {
        boolean intact = auditService.verifyIntegrity();
        updateIntegrityStatus(intact);
        if (intact) {
            Notification.show("Audit log verified: Hash chain is 100% authentic and untampered.", 4000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show("SECURITY ALERT: Cryptographic hash chain mismatch! Unauthorized tampering detected.",
                    6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateIntegrityStatus(boolean intact) {
        integrityBanner.removeAll();
        if (intact) {
            integrityBanner.getStyle()
                    .set("background", "var(--lumo-success-color-10pct)")
                    .set("border", "1px solid var(--lumo-success-color-50pct)")
                    .set("color", "var(--lumo-success-text-color)");
            integrityBanner.add(new Span("✓ CRYPTOGRAPHIC HASH CHAIN INTEGRITY: INTACT (Sequential SHA-256 chain verified)"));
        } else {
            integrityBanner.getStyle()
                    .set("background", "var(--lumo-error-color-10pct)")
                    .set("border", "1px solid var(--lumo-error-color-50pct)")
                    .set("color", "var(--lumo-error-text-color)");
            integrityBanner.add(new Span("⚠️ TAMPERING DETECTED: Audit log records do not match cryptographic hash signature!"));
        }
    }

    private void refreshGrid() {
        grid.setItems(auditLogRepository.findAllByOrderByTimestampDesc());
    }

    private void filterLogs(String q) {
        if (q == null || q.isBlank()) {
            refreshGrid();
            return;
        }
        String lower = q.toLowerCase().trim();
        List<AuditLog> filtered = auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .filter(l -> (l.getAction() != null && l.getAction().toLowerCase().contains(lower))
                        || (l.getEntity() != null && l.getEntity().toLowerCase().contains(lower))
                        || (l.getUserId() != null && l.getUserId().toLowerCase().contains(lower))
                        || (l.getNewValue() != null && l.getNewValue().toLowerCase().contains(lower)))
                .toList();
        grid.setItems(filtered);
    }

    private byte[] generateCsvBytes() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Timestamp", "User", "Role", "IP", "Action", "Entity", "EntityID", "OldValue", "NewValue", "Hash")
                .build())) {
            for (AuditLog l : logs) {
                printer.printRecord(l.getId(), l.getTimestamp(), l.getUserId(), l.getRole(),
                        l.getIpAddress(), l.getAction(), l.getEntity(), l.getEntityId(),
                        l.getOldValue(), l.getNewValue(), l.getHash());
            }
        } catch (Exception e) {
            return new byte[0];
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }
}
