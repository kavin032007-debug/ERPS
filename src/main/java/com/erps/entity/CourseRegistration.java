package com.erps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course_registrations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "course_id", "exam_id"})
})
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @Column(nullable = false, length = 50)
    private String status = "REGISTERED";

    @Column(name = "verified_by")
    private String verifiedBy;

    public CourseRegistration() {}

    public CourseRegistration(Student student, Course course, Examination exam, String status, String verifiedBy) {
        this.student = student;
        this.course = course;
        this.exam = exam;
        this.status = status != null ? status : "REGISTERED";
        this.verifiedBy = verifiedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public Examination getExamination() { return exam; }
    public void setExamination(Examination exam) { this.exam = exam; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
}
