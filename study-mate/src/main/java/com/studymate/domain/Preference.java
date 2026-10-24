package com.studymate.domain;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="study_preferences")
@Entity
public class Preference {

    @Id
    @Column(name="student_id")
    private Long id;

    @OneToOne
    @MapsId                          
    @JoinColumn(name = "student_id")
    @JsonIgnore                       
    private Student student; 

    @ElementCollection
    private List<TimeSlot> availableTimes = new ArrayList<TimeSlot>();

    @Column(name="course")
    String course;

    @ElementCollection
    @Column(name="study_styles")
    private List<String> studyStyles = new ArrayList<String>();


    @Column(name="preferred_group_size")
    int preferredGroupSize;


    @Column(name="meeting_mode")
    private String meetingMode;
    /**
     * "IN_PERSON"
     * "ONLINE"
     * "HYBRID"
     * "ANY"xs
     */


}