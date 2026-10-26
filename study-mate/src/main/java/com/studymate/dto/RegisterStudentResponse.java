package com.studymate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Response body for POST /api/students, including the generated API key. */
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
