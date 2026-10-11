package com.erps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "regulations")
public class Regulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "effective_from_batch", nullable = false)
    private Integer effectiveFromBatch;

    public Regulation() {}

    public Regulation(String code, String name, Integer effectiveFromBatch) {
        this.code = code;
        this.name = name;
        this.effectiveFromBatch = effectiveFromBatch;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getEffectiveFromBatch() { return effectiveFromBatch; }
    public void setEffectiveFromBatch(Integer effectiveFromBatch) { this.effectiveFromBatch = effectiveFromBatch; }
}
