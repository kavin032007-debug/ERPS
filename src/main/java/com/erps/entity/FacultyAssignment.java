package com.erps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"faculty_id", "course_id", "academic_year"})
})
public class FacultyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "academic_year", length = 50)
    private String academicYear;

    public FacultyAssignment() {}

    public FacultyAssignment(Faculty faculty, Course course, String academicYear) {
        this.faculty = faculty;
        this.course = course;
        this.academicYear = academicYear;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
}
