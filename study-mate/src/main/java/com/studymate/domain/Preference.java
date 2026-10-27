package com.studymate.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Partially AI-fixed (Claude)
/** A student's study preference; shares its primary key with the owning {@link Student}. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "preference")
@Entity
public class Preference {

  @Id
  @Column(name = "student_id")
  private Long id;

  @OneToOne
  @MapsId
  @JoinColumn(name = "student_id")
  @JsonIgnore
  private Student student;

  @ElementCollection private List<TimeSlot> availableTimes = new ArrayList<TimeSlot>();

  @Column(name = "course")
  String course;

  @ElementCollection
  @Column(name = "study_styles")
  private List<String> studyStyles = new ArrayList<String>();

  @Column(name = "preferred_group_size")
  int preferredGroupSize;

  /** One of "IN_PERSON", "ONLINE", "HYBRID", or "ANY". */
  @Column(name = "meeting_mode")
  private String meetingMode;
}
