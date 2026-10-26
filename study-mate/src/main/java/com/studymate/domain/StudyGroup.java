package com.studymate.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** A study group that students can find, join, and leave. */
@Getter
@Setter
@Entity
public class StudyGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long groupId;

  private String name;
  private String school;
  private String course;
  @ElementCollection private List<String> memberIds;
  private int maxMembers;
  private int numMembers;
  @ElementCollection private List<TimeSlot> schedules;
  @ElementCollection private List<String> studyStyles;
  private String meetingMode;
  private boolean isRecruiting;
}
