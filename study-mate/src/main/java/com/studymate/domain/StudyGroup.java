package com.studymate.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StudyGroup {
    private String groupId;
    private String name;
    private String course;
    private List<String> memberIds;
    private int maxMembers;
    private String schedule;
    private List<String> studyStyles;
    private String meetingMode;
}