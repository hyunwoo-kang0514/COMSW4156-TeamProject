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
    private String studentId;
    private String name;
    private String email;
    private String school;
    private String major;
    private int year;
    @OneToOne(mappedBy = "student")
    private Preference preference;
}