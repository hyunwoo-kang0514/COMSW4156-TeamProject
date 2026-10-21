package com.studymate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    private List<String> memberIds;
    private int maxMembers;
    private int numMembers;
    @ElementCollection
    private List<TimeSlot> schedules;
    private List<String> studyStyles;
    private String meetingMode;
    private boolean isRecruiting;
}