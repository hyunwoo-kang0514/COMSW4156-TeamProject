package com.studymate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class Preference {

    @Id
    private Long studentId;
    @OneToOne
    @MapsId
    @JoinColumn(
            name = "student_id",
            foreignKey = @ForeignKey(name = "fk_preference_student")
    )
    private Student student;
    private String course;
    @ElementCollection
    private List<TimeSlot> availableTimes;
    @ElementCollection
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