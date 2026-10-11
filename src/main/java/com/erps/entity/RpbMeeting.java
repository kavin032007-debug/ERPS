package com.erps.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rpb_meetings")
public class RpbMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Examination exam;

    @Column(name = "meeting_date", nullable = false)
    private LocalDateTime meetingDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String members;

    @Column(name = "university_nominee")
    private String universityNominee;

    @Column(name = "minutes_ref", nullable = false)
    private String minutesRef;

    @Column(name = "sign_off_by")
    private String signOffBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RpbMeeting() {}

    public RpbMeeting(Examination exam, LocalDateTime meetingDate, String members,
                      String universityNominee, String minutesRef, String signOffBy) {
        this.exam = exam;
        this.meetingDate = meetingDate;
        this.members = members;
        this.universityNominee = universityNominee;
        this.minutesRef = minutesRef;
        this.signOffBy = signOffBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Examination getExam() { return exam; }
    public void setExam(Examination exam) { this.exam = exam; }

    public LocalDateTime getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDateTime meetingDate) { this.meetingDate = meetingDate; }

    public String getMembers() { return members; }
    public void setMembers(String members) { this.members = members; }

    public String getUniversityNominee() { return universityNominee; }
    public void setUniversityNominee(String universityNominee) { this.universityNominee = universityNominee; }

    public String getMinutesRef() { return minutesRef; }
    public void setMinutesRef(String minutesRef) { this.minutesRef = minutesRef; }

    public String getSignOffBy() { return signOffBy; }
    public void setSignOffBy(String signOffBy) { this.signOffBy = signOffBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
