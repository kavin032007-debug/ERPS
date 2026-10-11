package com.erps.view;

import com.erps.entity.Department;
import com.erps.entity.Programme;
import com.erps.entity.Regulation;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "departments", layout = MainLayout.class)
@PageTitle("Departments & Programmes | ERPS")
@RolesAllowed({"SUPER_ADMIN", "TECH_STAFF", "EXAM_CELL_COORDINATOR"})
public class DepartmentProgrammeView extends VerticalLayout {

    private final MasterDataService masterDataService;
    private final Grid<Department> deptGrid = new Grid<>(Department.class, false);
    private final Grid<Programme> progGrid = new Grid<>(Programme.class, false);
    private final Grid<Regulation> regGrid = new Grid<>(Regulation.class, false);

    public DepartmentProgrammeView(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;

        setSizeFull();
        setPadding(true);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        tabSheet.add("Departments", createDepartmentTab());
        tabSheet.add("Programmes", createProgrammeTab());
        tabSheet.add("Regulations", createRegulationTab());

        add(tabSheet);
    }

    private VerticalLayout createDepartmentTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Button addBtn = new Button("Add Department", VaadinIcon.PLUS.create(), e -> openDeptDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(addBtn);

        deptGrid.addColumn(Department::getCode).setHeader("Code").setWidth("120px").setFlexGrow(0).setSortable(true);
        deptGrid.addColumn(Department::getName).setHeader("Department Name").setFlexGrow(1).setSortable(true);
        deptGrid.addComponentColumn(d -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDeptDialog(d));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return edit;
        }).setHeader("Actions").setWidth("100px").setFlexGrow(0);

        refreshDepts();
        layout.add(bar, deptGrid);
        return layout;
    }

    private void openDeptDialog(Department existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add Department" : "Edit Department");

        TextField codeField = new TextField("Department Code");
        TextField nameField = new TextField("Department Name");

        if (existing != null) {
            codeField.setValue(existing.getCode());
            nameField.setValue(existing.getName());
        }

        FormLayout form = new FormLayout(codeField, nameField);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            Department d = existing != null ? existing : new Department();
            d.setCode(codeField.getValue());
            d.setName(nameField.getValue());
            masterDataService.saveDepartment(d);
            Notification.show("Department saved successfully.");
            dialog.close();
            refreshDepts();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshDepts() {
        deptGrid.setItems(masterDataService.getAllDepartments());
    }

    private VerticalLayout createProgrammeTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Button addBtn = new Button("Add Programme", VaadinIcon.PLUS.create(), e -> openProgDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(addBtn);

        progGrid.addColumn(Programme::getCode).setHeader("Programme Code").setWidth("150px").setFlexGrow(0);
        progGrid.addColumn(Programme::getName).setHeader("Programme Title").setFlexGrow(1);
        progGrid.addColumn(p -> p.getDepartment() != null ? p.getDepartment().getName() : "-").setHeader("Department");

        refreshProgs();
        layout.add(bar, progGrid);
        return layout;
    }

    private void openProgDialog(Programme existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add Programme" : "Edit Programme");

        TextField codeField = new TextField("Programme Code");
        TextField nameField = new TextField("Programme Title");
        Select<Department> deptSelect = new Select<>();
        deptSelect.setLabel("Department");
        deptSelect.setItemLabelGenerator(Department::getName);
        deptSelect.setItems(masterDataService.getAllDepartments());

        if (existing != null) {
            codeField.setValue(existing.getCode());
            nameField.setValue(existing.getName());
            deptSelect.setValue(existing.getDepartment());
        }

        FormLayout form = new FormLayout(codeField, nameField, deptSelect);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            Programme p = existing != null ? existing : new Programme();
            p.setCode(codeField.getValue());
            p.setName(nameField.getValue());
            p.setDepartment(deptSelect.getValue());
            masterDataService.saveProgramme(p);
            Notification.show("Programme saved successfully.");
            dialog.close();
            refreshProgs();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshProgs() {
        progGrid.setItems(masterDataService.getAllProgrammes());
    }

    private VerticalLayout createRegulationTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Button addBtn = new Button("Add Regulation", VaadinIcon.PLUS.create(), e -> openRegDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(addBtn);

        regGrid.addColumn(Regulation::getCode).setHeader("Regulation Code").setWidth("150px").setFlexGrow(0);
        regGrid.addColumn(Regulation::getName).setHeader("Regulation Name").setFlexGrow(1);
        regGrid.addColumn(Regulation::getEffectiveFromBatch).setHeader("Effective From Batch Year");

        refreshRegs();
        layout.add(bar, regGrid);
        return layout;
    }

    private void openRegDialog(Regulation existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add Regulation" : "Edit Regulation");

        TextField codeField = new TextField("Regulation Code (e.g. R2024)");
        TextField nameField = new TextField("Regulation Name");
        IntegerField batchField = new IntegerField("Effective From Batch Year (e.g. 2024)");

        if (existing != null) {
            codeField.setValue(existing.getCode());
            nameField.setValue(existing.getName());
            batchField.setValue(existing.getEffectiveFromBatch());
        }

        FormLayout form = new FormLayout(codeField, nameField, batchField);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            Regulation r = existing != null ? existing : new Regulation();
            r.setCode(codeField.getValue());
            r.setName(nameField.getValue());
            r.setEffectiveFromBatch(batchField.getValue());
            masterDataService.saveRegulation(r);
            Notification.show("Regulation created with default absolute grading scale.");
            dialog.close();
            refreshRegs();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshRegs() {
        regGrid.setItems(masterDataService.getAllRegulations());
    }
}
