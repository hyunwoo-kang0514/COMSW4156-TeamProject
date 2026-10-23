package com.studymate.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String studentId;
    private String name;
    private String email;
    private String school;
    private String major;
    private int year;
    @OneToOne(mappedBy = "student")
    private Preference preference;
    // Client always to have exactly one apiKey
    @Column(unique = true, nullable = false)
    private String apiKey;
}