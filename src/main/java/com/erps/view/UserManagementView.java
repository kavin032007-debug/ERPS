package com.erps.view;

import com.erps.entity.*;
import com.erps.repository.FacultyAssignmentRepository;
import com.erps.repository.UserRepository;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("User & Faculty Management | ERPS")
@RolesAllowed("SUPER_ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserRepository userRepository;
    private final MasterDataService masterDataService;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    private final Grid<User> userGrid = new Grid<>(User.class, false);
    private final Grid<FacultyAssignment> assignGrid = new Grid<>(FacultyAssignment.class, false);

    public UserManagementView(UserRepository userRepository,
                              MasterDataService masterDataService,
                              FacultyAssignmentRepository facultyAssignmentRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.masterDataService = masterDataService;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setPadding(true);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        tabSheet.add("System Users & Roles", createUsersTab());
        tabSheet.add("Faculty Course Assignments", createAssignmentsTab());

        add(tabSheet);
    }

    private VerticalLayout createUsersTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Button addBtn = new Button("Create User", VaadinIcon.PLUS.create(), e -> openUserDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(addBtn);

        userGrid.addColumn(User::getEmail).setHeader("Email Address").setFlexGrow(1).setSortable(true);
        userGrid.addColumn(User::getFullName).setHeader("Full Name").setFlexGrow(1).setSortable(true);
        userGrid.addComponentColumn(u -> {
            Span roleBadge = new Span(u.getRole().name());
            roleBadge.getElement().getThemeList().add("badge primary small");
            return roleBadge;
        }).setHeader("Role").setWidth("180px").setFlexGrow(0);

        userGrid.addColumn(u -> Boolean.TRUE.equals(u.getMfaEnabled()) ? "Enabled" : "Off").setHeader("MFA Status").setWidth("120px");
        userGrid.addColumn(u -> Boolean.TRUE.equals(u.getEnabled()) ? "Active" : "Disabled").setHeader("Account Status").setWidth("120px");

        refreshUsers();
        layout.add(bar, userGrid);
        return layout;
    }

    private void openUserDialog(User existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Create New User" : "Edit User");

        TextField emailField = new TextField("Email Address");
        TextField nameField = new TextField("Full Name");
        PasswordField passField = new PasswordField("Password");
        Select<Role> roleSelect = new Select<>();
        roleSelect.setLabel("Role");
        roleSelect.setItems(Role.values());

        if (existing != null) {
            emailField.setValue(existing.getEmail());
            nameField.setValue(existing.getFullName());
            roleSelect.setValue(existing.getRole());
            passField.setHelperText("Leave blank to keep current password");
        } else {
            roleSelect.setValue(Role.FACULTY);
        }

        FormLayout form = new FormLayout(emailField, nameField, roleSelect, passField);
        dialog.add(form);

        Button save = new Button("Save User", e -> {
            User u = existing != null ? existing : new User();
            u.setEmail(emailField.getValue());
            u.setFullName(nameField.getValue());
            u.setRole(roleSelect.getValue());
            if (!passField.getValue().isBlank()) {
                u.setPasswordHash(passwordEncoder.encode(passField.getValue()));
            }
            userRepository.save(u);
            Notification.show("User account saved.");
            dialog.close();
            refreshUsers();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshUsers() {
        userGrid.setItems(userRepository.findAll());
    }

    private VerticalLayout createAssignmentsTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        HorizontalLayout bar = new HorizontalLayout();
        Button assignBtn = new Button("Assign Faculty to Course", VaadinIcon.LINK.create(), e -> openAssignDialog());
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(assignBtn);

        assignGrid.addColumn(a -> a.getFaculty().getName()).setHeader("Faculty Name").setFlexGrow(1);
        assignGrid.addColumn(a -> a.getFaculty().getEmail()).setHeader("Faculty Email").setFlexGrow(1);
        assignGrid.addColumn(a -> a.getCourse().getCode() + " - " + a.getCourse().getTitle()).setHeader("Course").setFlexGrow(2);
        assignGrid.addColumn(FacultyAssignment::getAcademicYear).setHeader("Academic Year").setWidth("140px");

        refreshAssignments();
        layout.add(bar, assignGrid);
        return layout;
    }

    private void openAssignDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Assign Faculty to Course");

        Select<Faculty> facultySelect = new Select<>();
        facultySelect.setLabel("Select Faculty");
        facultySelect.setItemLabelGenerator(f -> f.getName() + " (" + f.getEmail() + ")");
        facultySelect.setItems(masterDataService.getAllFaculty());

        Select<Course> courseSelect = new Select<>();
        courseSelect.setLabel("Select Course");
        courseSelect.setItemLabelGenerator(c -> c.getCode() + " - " + c.getTitle());
        courseSelect.setItems(masterDataService.getAllCourses());

        TextField yearField = new TextField("Academic Year", "2025-2026", "");

        FormLayout form = new FormLayout(facultySelect, courseSelect, yearField);
        dialog.add(form);

        Button save = new Button("Assign", e -> {
            masterDataService.assignFacultyToCourse(facultySelect.getValue(), courseSelect.getValue(), yearField.getValue());
            Notification.show("Faculty assigned to course successfully.");
            dialog.close();
            refreshAssignments();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshAssignments() {
        assignGrid.setItems(facultyAssignmentRepository.findAll());
    }
}
