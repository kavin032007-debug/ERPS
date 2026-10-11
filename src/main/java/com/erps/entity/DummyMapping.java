package com.erps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dummy_mappings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "course_id", "dummy_id"}),
    @UniqueConstraint(columnNames = {"exam_id", "course_id", "student_id"})
})
public class DummyMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "dummy_id", nullable = false, length = 50)
    private String dummyId;

    public DummyMapping() {}

    public DummyMapping(Examination exam, Student student, Course course, String dummyId) {
        this.exam = exam;
        this.student = student;
        this.course = course;
        this.dummyId = dummyId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public Examination getExamination() { return exam; }
    public void setExamination(Examination exam) { this.exam = exam; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getDummyId() { return dummyId; }
    public void setDummyId(String dummyId) { this.dummyId = dummyId; }
}
