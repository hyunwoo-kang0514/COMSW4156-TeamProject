package com.studymate.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.studymate.domain.TimeSlot;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStudyGroupRequest {

  private Long creatorStudentId;
  private String name;
  private String course;
  private Integer maxMembers;
  private List<TimeSlot> schedules;
  private List<String> studyStyles;
  private String meetingMode;

  /**
   * Rejects fields outside the creation contract, including server-controlled values.
   *
   * <p>This also rejects unknown fields when the JSON mapper normally ignores them.
   *
   * @param fieldName the unrecognized JSON field
   * @param value the supplied value, which is never accepted or stored
   * @throws IllegalArgumentException whenever an unrecognized field is supplied
   */
  @JsonAnySetter
  public void rejectUnknownField(String fieldName, Object value) {
    throw new IllegalArgumentException(
        "Field '" + fieldName + "' is not allowed when creating a study group.");
  }
}
