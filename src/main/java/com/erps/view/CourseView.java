package com.erps.view;

import com.erps.entity.Course;
import com.erps.entity.CourseType;
import com.erps.entity.Semester;
import com.erps.service.MasterDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "courses", layout = MainLayout.class)
@PageTitle("Courses & Semesters | ERPS")
@RolesAllowed({"SUPER_ADMIN", "TECH_STAFF", "EXAM_CELL_COORDINATOR"})
public class CourseView extends VerticalLayout {

    private final MasterDataService masterDataService;
    private final Grid<Course> courseGrid = new Grid<>(Course.class, false);

    public CourseView(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;

        setSizeFull();
        setPadding(true);

        HorizontalLayout bar = new HorizontalLayout();
        Button addBtn = new Button("Add New Course", VaadinIcon.PLUS.create(), e -> openCourseDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(addBtn);

        courseGrid.addColumn(Course::getCode).setHeader("Course Code").setWidth("130px").setFlexGrow(0).setSortable(true);
        courseGrid.addColumn(Course::getTitle).setHeader("Course Title").setFlexGrow(1).setSortable(true);
        courseGrid.addColumn(c -> "Sem " + c.getSemester()).setHeader("Semester").setWidth("100px").setFlexGrow(0);
        courseGrid.addColumn(c -> c.getCredits() + " cr").setHeader("Credits").setWidth("100px").setFlexGrow(0);
        courseGrid.addColumn(Course::getType).setHeader("Type").setWidth("120px").setFlexGrow(0);
        courseGrid.addColumn(c -> c.getCiaMax() + " / " + c.getSeeMax()).setHeader("CIA / SEE Max").setWidth("130px").setFlexGrow(0);
        courseGrid.addColumn(c -> c.getCiaWeight() + "% / " + c.getSeeWeight() + "%").setHeader("Weightage").setWidth("130px").setFlexGrow(0);

        courseGrid.addComponentColumn(c -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openCourseDialog(c));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return edit;
        }).setHeader("Actions").setWidth("100px").setFlexGrow(0);

        refreshCourses();
        add(bar, courseGrid);
    }

    private void openCourseDialog(Course existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add New Course" : "Edit Course: " + existing.getCode());

        TextField codeField = new TextField("Course Code");
        TextField titleField = new TextField("Course Title");
        BigDecimalField creditsField = new BigDecimalField("Credits");
        IntegerField semesterField = new IntegerField("Semester");
        Select<CourseType> typeSelect = new Select<>();
        typeSelect.setLabel("Course Type");
        typeSelect.setItems(CourseType.values());

        BigDecimalField ciaMaxField = new BigDecimalField("CIA Max Marks");
        BigDecimalField seeMaxField = new BigDecimalField("SEE Max Marks");
        BigDecimalField ciaWeightField = new BigDecimalField("CIA Weight %");
        BigDecimalField seeWeightField = new BigDecimalField("SEE Weight %");
        BigDecimalField minSeeField = new BigDecimalField("Optional Min SEE Marks (or empty)");

        if (existing != null) {
            codeField.setValue(existing.getCode());
            titleField.setValue(existing.getTitle());
            creditsField.setValue(existing.getCredits());
            semesterField.setValue(existing.getSemester());
            typeSelect.setValue(existing.getType());
            ciaMaxField.setValue(existing.getCiaMax());
            seeMaxField.setValue(existing.getSeeMax());
            ciaWeightField.setValue(existing.getCiaWeight());
            seeWeightField.setValue(existing.getSeeWeight());
            minSeeField.setValue(existing.getMinSeeMarks());
        } else {
            creditsField.setValue(BigDecimal.valueOf(4.0));
            semesterField.setValue(3);
            typeSelect.setValue(CourseType.THEORY);
            ciaMaxField.setValue(BigDecimal.valueOf(40.0));
            seeMaxField.setValue(BigDecimal.valueOf(60.0));
            ciaWeightField.setValue(BigDecimal.valueOf(40.0));
            seeWeightField.setValue(BigDecimal.valueOf(60.0));
        }

        FormLayout form = new FormLayout(
                codeField, titleField, creditsField, semesterField, typeSelect,
                ciaMaxField, seeMaxField, ciaWeightField, seeWeightField, minSeeField
        );
        dialog.add(form);

        Button save = new Button("Save Course", e -> {
            Course c = existing != null ? existing : new Course();
            c.setCode(codeField.getValue());
            c.setTitle(titleField.getValue());
            c.setCredits(creditsField.getValue());
            c.setSemester(semesterField.getValue());
            c.setType(typeSelect.getValue());
            c.setCiaMax(ciaMaxField.getValue());
            c.setSeeMax(seeMaxField.getValue());
            c.setCiaWeight(ciaWeightField.getValue());
            c.setSeeWeight(seeWeightField.getValue());
            c.setMinSeeMarks(minSeeField.getValue());

            masterDataService.saveCourse(c);
            Notification.show("Course saved successfully.");
            dialog.close();
            refreshCourses();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refreshCourses() {
        courseGrid.setItems(masterDataService.getAllCourses());
    }
}
