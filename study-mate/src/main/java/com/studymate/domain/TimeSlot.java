package com.studymate.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** A weekly time window, used for student availability and group schedules. */
@Embeddable
@Getter
@Setter
public class TimeSlot {
  @Enumerated(EnumType.STRING)
  private DayOfWeek day;

  private LocalTime startTime;
  private LocalTime endTime;
}
