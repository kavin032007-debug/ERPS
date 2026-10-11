package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mark_alterations")
public class MarkAlteration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "marks_id", nullable = false)
    private Marks marks;

    @Column(name = "old_value", precision = 5, scale = 2)
    private BigDecimal oldValue;

    @Column(name = "new_value", precision = 5, scale = 2)
    private BigDecimal newValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlterationStatus status = AlterationStatus.PENDING;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "signed_by")
    private String signedBy;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public MarkAlteration() {}

    public MarkAlteration(Marks marks, BigDecimal oldValue, BigDecimal newValue, String reason, String requestedBy) {
        this.marks = marks;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.status = AlterationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Marks getMarks() { return marks; }
    public void setMarks(Marks marks) { this.marks = marks; }

    public BigDecimal getOldValue() { return oldValue; }
    public void setOldValue(BigDecimal oldValue) { this.oldValue = oldValue; }

    public BigDecimal getNewValue() { return newValue; }
    public void setNewValue(BigDecimal newValue) { this.newValue = newValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public AlterationStatus getStatus() { return status; }
    public void setStatus(AlterationStatus status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
