package com.studymate.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class StudyGroup {
    private Long groupId;
    private String name;
    private String course;
    private List<String> memberIds;
    private int maxMembers;
    private int numMembers;
    private List<TimeSlot> schedules;
    private List<String> studyStyles;
    private String meetingMode;
    private boolean isRecruiting;
}