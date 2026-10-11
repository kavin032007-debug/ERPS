package com.erps.view;

import com.erps.entity.*;
import com.erps.security.AuthenticatedUser;
import com.erps.service.ExaminationService;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "students", layout = MainLayout.class)
@PageTitle("Student Profiles & Registrations | ERPS")
@RolesAllowed({"SUPER_ADMIN", "TECH_STAFF", "EXAM_CELL_COORDINATOR"})
public class StudentManagementView extends VerticalLayout {

    private final MasterDataService masterDataService;
    private final ExaminationService examinationService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<Student> studentGrid = new Grid<>(Student.class, false);
    private final Grid<CourseRegistration> regGrid = new Grid<>(CourseRegistration.class, false);

    public StudentManagementView(MasterDataService masterDataService,
                                 ExaminationService examinationService,
                                 AuthenticatedUser authenticatedUser) {
        this.masterDataService = masterDataService;
        this.examinationService = examinationService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        tabSheet.add("Student Profiles", createStudentsTab());
        tabSheet.add("Course Registrations", createRegistrationsTab());
        tabSheet.add("CSV Bulk Import", createCsvImportTab());

        add(tabSheet);
    }

    private VerticalLayout createStudentsTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Button addBtn = new Button("Add Student Profile", VaadinIcon.PLUS.create(), e -> openStudentDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search by register no or name...");
        searchField.setWidth("300px");
        searchField.addValueChangeListener(e -> filterStudents(e.getValue()));

        bar.add(addBtn, searchField);

        studentGrid.addColumn(Student::getRegisterNo).setHeader("Register No").setWidth("140px").setFlexGrow(0).setSortable(true);
        studentGrid.addColumn(Student::getName).setHeader("Student Name").setFlexGrow(1).setSortable(true);
        studentGrid.addColumn(Student::getEmail).setHeader("Email").setFlexGrow(1);
        studentGrid.addColumn(s -> s.getProgramme().getCode()).setHeader("Programme").setWidth("140px").setFlexGrow(0);
        studentGrid.addColumn(Student::getBatchYear).setHeader("Batch").setWidth("100px").setFlexGrow(0);
        studentGrid.addColumn(s -> "Sem " + s.getCurrentSemester()).setHeader("Current Sem").setWidth("120px").setFlexGrow(0);
        studentGrid.addColumn(s -> s.getRegulation().getCode()).setHeader("Regulation").setWidth("120px").setFlexGrow(0);

        studentGrid.addComponentColumn(s -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openStudentDialog(s));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return edit;
        }).setHeader("Actions").setWidth("100px").setFlexGrow(0);

        refreshStudents();
        layout.add(bar, studentGrid);
        return layout;
    }

    private void filterStudents(String query) {
        if (query == null || query.isBlank()) {
            refreshStudents();
            return;
        }
        String q = query.toLowerCase().trim();
        List<Student> filtered = masterDataService.getAllStudents().stream()
                .filter(s -> s.getRegisterNo().toLowerCase().contains(q) || s.getName().toLowerCase().contains(q))
                .toList();
        studentGrid.setItems(filtered);
    }

    private void openStudentDialog(Student existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add Student" : "Edit Student: " + existing.getRegisterNo());

        TextField regNoField = new TextField("Register Number");
        TextField nameField = new TextField("Full Name");
        TextField emailField = new TextField("Email");

        Select<Department> deptSelect = new Select<>();
        deptSelect.setLabel("Department");
        deptSelect.setItemLabelGenerator(Department::getName);
        deptSelect.setItems(masterDataService.getAllDepartments());

        Select<Programme> progSelect = new Select<>();
        progSelect.setLabel("Programme");
        progSelect.setItemLabelGenerator(Programme::getName);
        progSelect.setItems(masterDataService.getAllProgrammes());

        Select<Regulation> regSelect = new Select<>();
        regSelect.setLabel("Regulation");
        regSelect.setItemLabelGenerator(r -> r.getCode() + " (" + r.getName() + ")");
        regSelect.setItems(masterDataService.getAllRegulations());

        IntegerField batchField = new IntegerField("Batch Year (e.g. 2024)");
        IntegerField semField = new IntegerField("Current Semester (e.g. 3)");

        if (existing != null) {
            regNoField.setValue(existing.getRegisterNo());
            nameField.setValue(existing.getName());
            emailField.setValue(existing.getEmail());
            deptSelect.setValue(existing.getDepartment());
            progSelect.setValue(existing.getProgramme());
            regSelect.setValue(existing.getRegulation());
            batchField.setValue(existing.getBatchYear());
            semField.setValue(existing.getCurrentSemester());
        }

        FormLayout form = new FormLayout(regNoField, nameField, emailField, deptSelect, progSelect, regSelect, batchField, semField);
        dialog.add(form);

        Button save = new Button("Save Student", e -> {
            Student s = existing != null ? existing : new Student();
            s.setRegisterNo(regNoField.getValue());
            s.setName(nameField.getValue());
            s.setEmail(emailField.getValue());
            s.setDepartment(deptSelect.getValue());
            s.setProgramme(progSelect.getValue());
            s.setRegulation(regSelect.getValue());
            s.setBatchYear(batchField.getValue());
            s.setCurrentSemester(semField.getValue());

            masterDataService.saveStudent(s, "Password@123");
            Notification.show("Student record saved successfully.");
            dialog.close();
            refreshStudents();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshStudents() {
        studentGrid.setItems(masterDataService.getAllStudents());
    }

    private VerticalLayout createRegistrationsTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Select<Examination> examSelect = new Select<>();
        examSelect.setLabel("Select Examination");
        examSelect.setItemLabelGenerator(Examination::getName);
        examSelect.setItems(examinationService.getAllExaminations());
        examSelect.setWidth("350px");

        examSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                regGrid.setItems(masterDataService.getRegistrationsForExam(e.getValue().getId()));
            }
        });

        bar.add(examSelect);

        regGrid.addColumn(r -> r.getStudent().getRegisterNo()).setHeader("Register No").setWidth("140px").setFlexGrow(0);
        regGrid.addColumn(r -> r.getStudent().getName()).setHeader("Student Name").setFlexGrow(1);
        regGrid.addColumn(r -> r.getCourse().getCode()).setHeader("Course Code").setWidth("130px").setFlexGrow(0);
        regGrid.addColumn(r -> r.getCourse().getTitle()).setHeader("Course Title").setFlexGrow(1);
        regGrid.addColumn(CourseRegistration::getStatus).setHeader("Registration Status").setWidth("140px").setFlexGrow(0);
        regGrid.addColumn(r -> r.getVerifiedBy() != null ? r.getVerifiedBy() : "Pending").setHeader("Verified By");

        regGrid.addComponentColumn(r -> {
            if ("REGISTERED".equals(r.getStatus())) {
                Button verifyBtn = new Button("Verify", VaadinIcon.CHECK.create(), e -> {
                    String coordinator = authenticatedUser.get().map(User::getFullName).orElse("Coordinator");
                    masterDataService.verifyRegistration(r.getId(), coordinator);
                    Notification.show("Course registration verified.");
                    if (examSelect.getValue() != null) {
                        regGrid.setItems(masterDataService.getRegistrationsForExam(examSelect.getValue().getId()));
                    }
                });
                verifyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                return verifyBtn;
            }
            Span verifiedBadge = new Span("Verified");
            verifiedBadge.getElement().getThemeList().add("badge success small");
            return verifiedBadge;
        }).setHeader("Action").setWidth("120px").setFlexGrow(0);

        if (!examinationService.getAllExaminations().isEmpty()) {
            examSelect.setValue(examinationService.getAllExaminations().get(0));
        }

        layout.add(bar, regGrid);
        return layout;
    }

    private VerticalLayout createCsvImportTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setSpacing(true);

        H3 title = new H3("CSV Bulk Import: Student Profiles (FR2)");
        Span headerSpec = new Span("Expected CSV Header: register_no,name,email,department_code,programme_code,regulation_code,batch_year,current_semester");
        headerSpec.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        TextArea csvArea = new TextArea("Paste CSV Data Here");
        csvArea.setWidthFull();
        csvArea.setHeight("220px");
        csvArea.setValue("""
register_no,name,email,department_code,programme_code,regulation_code,batch_year,current_semester
24CS031,Karthik Raman,karthik@institution.edu,CSE,BTECH-CSE,R2024,2024,3
24CS032,Lakshmi Priya,lakshmi@institution.edu,CSE,BTECH-CSE,R2024,2024,3
24CS033,Manoj Kumar,manoj@institution.edu,CSE,BTECH-CSE,R2024,2024,3
""");

        TextArea logArea = new TextArea("Row-Level Import Results");
        logArea.setWidthFull();
        logArea.setHeight("200px");
        logArea.setReadOnly(true);

        Button importBtn = new Button("Execute Bulk Import", VaadinIcon.UPLOAD.create(), e -> {
            String content = csvArea.getValue();
            if (content == null || content.isBlank()) {
                Notification.show("Please paste CSV data first.", 3000, Notification.Position.MIDDLE);
                return;
            }
            List<String> logs = masterDataService.bulkImportStudentsCsv(content);
            logArea.setValue(String.join("\n", logs));
            Notification.show("Import processed. Check row-level log.", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            refreshStudents();
        });
        importBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(title, headerSpec, csvArea, importBtn, logArea);
        return layout;
    }
}
