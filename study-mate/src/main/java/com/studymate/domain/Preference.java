package com.studymate.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Preference {
    private String course;
    private String schedule;
    private List<String> studyStyles;
    private int preferredGroupSize;
    private String meetingMode;
}