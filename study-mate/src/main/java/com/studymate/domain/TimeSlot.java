package com.studymate.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
public class TimeSlot {
    @Enumerated(EnumType.STRING)
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;
}