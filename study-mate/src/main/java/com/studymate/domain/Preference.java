package com.studymate.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Table(name="study_preferences")
@Entity
public class Preference {

    @Id
    @Column(name="student_id")
    private Long studentId;

    @Column(name="school")
    private String school;

    @Column(name="year")
    long year;

    @Column(name="course")
    String course;

    @Column(name="schedule")
    Date schedule; //maybe have a bunch of times the student this preference is attached to is available for??

    @Column(name="preferred_group_size")
    int preferredGroupSize;


}