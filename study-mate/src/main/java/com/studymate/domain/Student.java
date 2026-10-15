package com.studymate.domain;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Student {
    private String studentId;
    private String name;
    private String email;
    private String school;
    private String major;
    private int year;
    private Preference preference;
}