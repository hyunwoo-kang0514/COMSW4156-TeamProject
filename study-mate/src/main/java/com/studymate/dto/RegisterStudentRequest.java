package com.studymate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterStudentRequest {

    private String studentId;
    private String name;
    private String email;
    private String school;
    private String major;
    private int year;
}