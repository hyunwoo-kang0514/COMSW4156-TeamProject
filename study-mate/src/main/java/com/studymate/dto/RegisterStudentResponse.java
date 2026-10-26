package com.studymate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterStudentResponse {

    private Long id;
    private String studentId;
    private String name;
    private String email;
    private String school;
    private String major;
    private int year;
    private String apiKey;
}