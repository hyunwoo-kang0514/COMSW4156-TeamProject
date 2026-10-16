package com.studymate.domain;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Student {
    private String studentId;
    private String name;
    private String email;
    private String school;
    private String major;
    private int year;
    private Preference preference;
}