package com.erps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "grading_configs")
public class GradingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "regulation_id", nullable = false, unique = true)
    private Regulation regulation;

    @Column(name = "pass_rules")
    private String passRules = "NO_U_AND_NO_AB";

    @Column(name = "min_see_marks_enabled")
    private Boolean minSeeMarksEnabled = false;

    @Column(name = "default_min_see_marks", precision = 5, scale = 2)
    private BigDecimal defaultMinSeeMarks = BigDecimal.valueOf(27.00);

    @Column(name = "rank_requires_no_arrear")
    private Boolean rankRequiresNoArrear = true;

    @Column(name = "reval_window_days")
    private Integer revalWindowDays = 15;

    @Column(name = "reval_window_start_event", length = 100)
    private String revalWindowStartEvent = "RESULT_PUBLICATION";

    @Column(name = "publish_sla_days")
    private Integer publishSlaDays = 3;

    public GradingConfig() {}

    public GradingConfig(Regulation regulation) {
        this.regulation = regulation;
        this.passRules = "NO_U_AND_NO_AB";
        this.minSeeMarksEnabled = false;
        this.defaultMinSeeMarks = BigDecimal.valueOf(27.00);
        this.rankRequiresNoArrear = true;
        this.revalWindowDays = 15;
        this.revalWindowStartEvent = "RESULT_PUBLICATION";
        this.publishSlaDays = 3;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Regulation getRegulation() { return regulation; }
    public void setRegulation(Regulation regulation) { this.regulation = regulation; }

    public String getPassRules() { return passRules; }
    public void setPassRules(String passRules) { this.passRules = passRules; }

    public Boolean getMinSeeMarksEnabled() { return minSeeMarksEnabled != null && minSeeMarksEnabled; }
    public void setMinSeeMarksEnabled(Boolean minSeeMarksEnabled) { this.minSeeMarksEnabled = minSeeMarksEnabled; }

    public BigDecimal getDefaultMinSeeMarks() { return defaultMinSeeMarks; }
    public void setDefaultMinSeeMarks(BigDecimal defaultMinSeeMarks) { this.defaultMinSeeMarks = defaultMinSeeMarks; }

    public Boolean getRankRequiresNoArrear() { return rankRequiresNoArrear != null && rankRequiresNoArrear; }
    public void setRankRequiresNoArrear(Boolean rankRequiresNoArrear) { this.rankRequiresNoArrear = rankRequiresNoArrear; }

    public Integer getRevalWindowDays() { return revalWindowDays; }
    public void setRevalWindowDays(Integer revalWindowDays) { this.revalWindowDays = revalWindowDays; }

    public String getRevalWindowStartEvent() { return revalWindowStartEvent; }
    public void setRevalWindowStartEvent(String revalWindowStartEvent) { this.revalWindowStartEvent = revalWindowStartEvent; }

    public Integer getPublishSlaDays() { return publishSlaDays; }
    public void setPublishSlaDays(Integer publishSlaDays) { this.publishSlaDays = publishSlaDays; }
}
