/**
 * Request body for student registration. Developed with assistance from OpenAI Codex.
 * See README "AI Documentation" section for details.
 */

package com.studymate.dto;

import lombok.Getter;
import lombok.Setter;

/** Request body for POST /api/students. */
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
