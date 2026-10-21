package com.studymate.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
public class TimeSlot {

    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;
}