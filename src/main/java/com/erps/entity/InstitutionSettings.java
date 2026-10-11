package com.erps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "institution_settings")
public class InstitutionSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "logo_ref")
    private String logoRef;

    @Column(name = "university_name", nullable = false)
    private String universityName;

    public InstitutionSettings() {}

    public InstitutionSettings(String name, String address, String logoRef, String universityName) {
        this.name = name;
        this.address = address;
        this.logoRef = logoRef;
        this.universityName = universityName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLogoRef() { return logoRef; }
    public void setLogoRef(String logoRef) { this.logoRef = logoRef; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
}
