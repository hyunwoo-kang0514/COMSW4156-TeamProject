package com.studymate.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class Preference {
    private String course;
    private List<TimeSlot> availableTimes;
    private List<String> studyStyles;
    private int preferredGroupSize;
    private String meetingMode;
    /**
     * "IN_PERSON"
     * "ONLINE"
     * "HYBRID"
     * "ANY"
     */
}