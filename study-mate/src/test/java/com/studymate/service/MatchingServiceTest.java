package com.studymate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for scoring and ranking in {@link MatchingService}. */
@ExtendWith(MockitoExtension.class)
public class MatchingServiceTest {

  @Mock private StudyGroupService studyGroupService;

  @InjectMocks private MatchingService matchingService;

  private Student student;
  private Student student2;
  private StudyGroup studyGroup;
  private StudyGroup studyGroup2;
  private StudyGroup studyGroup3;
  private TimeSlot timeSlot;
  private TimeSlot timeSlot2;

  @BeforeEach
  void setUp() {
    // time slot 1
    timeSlot = new TimeSlot();
    timeSlot.setDay(DayOfWeek.MONDAY);
    timeSlot.setStartTime(LocalTime.of(14, 0));
    timeSlot.setEndTime(LocalTime.of(16, 0));
    // time slot 2
    timeSlot2 = new TimeSlot();
    timeSlot2.setDay(DayOfWeek.THURSDAY);
    timeSlot2.setStartTime(LocalTime.of(18, 30));
    timeSlot2.setEndTime(LocalTime.of(20, 30));
    List<TimeSlot> timeslots = new ArrayList<>();
    timeslots.add(timeSlot);
    timeslots.add(timeSlot2);

    // student 1
    student = new Student();
    student.setId(1L);
    student.setStudentId("hk3382");
    student.setName("Harry");
    student.setEmail("harry@example.com");
    student.setSchool("Columbia");
    student.setMajor("Computer Science");
    student.setYear(4);
    student.setApiKey("valid-key1");

    // student 2
    student2 = new Student();
    student2.setId(2L);
    student2.setStudentId("jw2022");
    student2.setName("Jun");
    student2.setEmail("Jun@example.com");
    student2.setSchool("Columbia");
    student2.setMajor("Computer Science");
    student2.setYear(3);
    student2.setApiKey("valid-key2");

    // study group 1
    studyGroup = new StudyGroup();
    studyGroup.setGroupId(1L);
    studyGroup.setName("COMS 4156 Study Group");
    studyGroup.setSchool("Columbia");
    studyGroup.setCourse("COMS4156");
    List<String> membersIds = new ArrayList<>();
    membersIds.add(student.getStudentId());
    membersIds.add(student2.getStudentId());
    studyGroup.setMemberIds(membersIds);
    studyGroup.setMaxMembers(4);
    studyGroup.setNumMembers(2);
    studyGroup.setSchedules(timeslots);
    List<String> styles = new ArrayList<>();
    styles.add("collaborative");
    styles.add("focused");
    studyGroup.setStudyStyles(styles);
    studyGroup.setMeetingMode("in-person");
    studyGroup.setRecruiting(true);

    // study group 1
    studyGroup3 = new StudyGroup();
    studyGroup3.setGroupId(1L);
    studyGroup3.setName("I love ASE");
    studyGroup3.setSchool("Columbia");
    studyGroup3.setCourse("COMS4156");
    studyGroup3.setMemberIds(membersIds);
    studyGroup3.setMaxMembers(10);
    studyGroup3.setNumMembers(5);
    studyGroup.setSchedules(timeslots);
    List<String> styles3 = new ArrayList<>();
    styles3.add("focused");
    studyGroup.setStudyStyles(styles3);
    studyGroup.setMeetingMode("in-person");
    studyGroup.setRecruiting(true);

    // study group 2
    studyGroup2 = new StudyGroup();
    studyGroup2.setGroupId(2L);
    studyGroup2.setName("COMS 4111 Study Group");
    studyGroup2.setSchool("Columbia");
    studyGroup2.setCourse("COMS4111");
    List<String> memberIds2 = new ArrayList<>();
    memberIds2.add(student2.getStudentId());
    studyGroup2.setMemberIds(memberIds2);
    studyGroup2.setMaxMembers(3);
    studyGroup2.setNumMembers(1);
    TimeSlot group2TimeSlot1 = new TimeSlot();
    group2TimeSlot1.setDay(DayOfWeek.TUESDAY);
    group2TimeSlot1.setStartTime(LocalTime.of(15, 0));
    group2TimeSlot1.setEndTime(LocalTime.of(17, 0));
    List<TimeSlot> timeSlots2 = new ArrayList<>();
    timeSlots2.add(group2TimeSlot1);
    TimeSlot group2TimeSlot2 = new TimeSlot();
    group2TimeSlot2.setDay(DayOfWeek.FRIDAY);
    group2TimeSlot2.setStartTime(LocalTime.of(10, 0));
    group2TimeSlot2.setEndTime(LocalTime.of(12, 0));
    timeSlots2.add(group2TimeSlot2);
    studyGroup2.setSchedules(timeSlots2);
    List<String> styles2 = new ArrayList<>();
    styles2.add("independent");
    styles2.add("focused");
    studyGroup2.setStudyStyles(styles2);
    studyGroup2.setMeetingMode("online");
    studyGroup2.setRecruiting(true);
  }

  @Test
  void findCompatibleStudyGroups() {
    // Arrange
    List<StudyGroup> candidates = new ArrayList<>();
    candidates.add(studyGroup);
    candidates.add(studyGroup3);
    when(studyGroupService.getCandidateGroups("Columbia", "COMS4156", "in-person"))
        .thenReturn(candidates);

    // Act
    List<StudyGroup> result =
        studyGroupService.getCandidateGroups("Columbia", "COMS4156", "in-person");

    // Assert
    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void findCompatibleStudyGroupsWithDoesNotMatchBasicCriteria() {
    // Arrange
    List<StudyGroup> candidates = new ArrayList<>();
    candidates.add(studyGroup);
    candidates.add(studyGroup3);
    when(studyGroupService.getCandidateGroups("Columbia", "COMS4156", "in-person"))
        .thenReturn(null);
    // Act
    List<StudyGroup> result =
        studyGroupService.getCandidateGroups("Columbia", "COMS4156", "in-person");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  void getScores() {
    // Arrange - student preference
    Preference preference = new Preference();
    preference.setCourse("COMS4156");
    preference.setMeetingMode("in-person");
    preference.setPreferredGroupSize(4);
    preference.setStudyStyles(new ArrayList<>(List.of("collaborative", "focused")));
    preference.setAvailableTimes(new ArrayList<>(List.of(timeSlot, timeSlot2)));
    student.setPreference(preference);

    // Group 1
    // size: 25
    // style: 25
    // time: 30
    // total: 80
    StudyGroup group1 = new StudyGroup();
    group1.setGroupId(11L);
    group1.setName("ASE Warriors");
    group1.setSchool("Columbia");
    group1.setCourse("COMS4156");
    group1.setMemberIds(new ArrayList<>(List.of("student1")));
    group1.setMaxMembers(4);
    group1.setNumMembers(1);
    group1.setSchedules(new ArrayList<>(List.of(timeSlot, timeSlot2)));
    group1.setStudyStyles(new ArrayList<>(List.of("collaborative", "focused")));
    group1.setMeetingMode("in-person");
    group1.setRecruiting(true);

    // Group 2
    // size: 20
    // style: 20
    // time: 30
    // total: 70
    StudyGroup group2 = new StudyGroup();
    group2.setGroupId(12L);
    group2.setName("Spring Boot Squad");
    group2.setSchool("Columbia");
    group2.setCourse("COMS4156");
    group2.setMemberIds(new ArrayList<>(List.of("student2")));
    group2.setMaxMembers(5);
    group2.setNumMembers(1);

    TimeSlot group2Time = new TimeSlot();
    group2Time.setDay(DayOfWeek.MONDAY);
    group2Time.setStartTime(LocalTime.of(14, 0));
    group2Time.setEndTime(LocalTime.of(16, 0));

    group2.setSchedules(new ArrayList<>(List.of(group2Time)));
    group2.setStudyStyles(new ArrayList<>(List.of("focused", "independent")));
    group2.setMeetingMode("in-person");
    group2.setRecruiting(true);

    // Group 3
    // size: 25
    // style: 25
    // time: 25
    // total: 75
    StudyGroup group3 = new StudyGroup();
    group3.setGroupId(13L);
    group3.setName("Mockito Masters");
    group3.setSchool("Columbia");
    group3.setCourse("COMS4156");
    group3.setMemberIds(new ArrayList<>(List.of("student3")));
    group3.setMaxMembers(4);
    group3.setNumMembers(1);

    TimeSlot group3Time = new TimeSlot();
    group3Time.setDay(DayOfWeek.MONDAY);
    group3Time.setStartTime(LocalTime.of(15, 0));
    group3Time.setEndTime(LocalTime.of(16, 0));

    group3.setSchedules(new ArrayList<>(List.of(group3Time)));
    group3.setStudyStyles(new ArrayList<>(List.of("collaborative", "focused")));
    group3.setMeetingMode("in-person");
    group3.setRecruiting(true);

    // Group 4
    // size: 0
    // style: 20
    // time: 15
    // total: 35
    StudyGroup group4 = new StudyGroup();
    group4.setGroupId(14L);
    group4.setName("REST Rangers");
    group4.setSchool("Columbia");
    group4.setCourse("COMS4156");
    group4.setMemberIds(new ArrayList<>(List.of("student4")));
    group4.setMaxMembers(8);
    group4.setNumMembers(1);

    TimeSlot group4Time = new TimeSlot();
    group4Time.setDay(DayOfWeek.THURSDAY);
    group4Time.setStartTime(LocalTime.of(20, 0));
    group4Time.setEndTime(LocalTime.of(20, 30));

    group4.setSchedules(new ArrayList<>(List.of(group4Time)));
    group4.setStudyStyles(new ArrayList<>(List.of("focused", "independent")));
    group4.setMeetingMode("in-person");
    group4.setRecruiting(true);

    // Group 5
    // size: 25
    // style: 0
    // time: 0
    // total: 25
    StudyGroup group5 = new StudyGroup();
    group5.setGroupId(15L);
    group5.setName("Java Giants");
    group5.setSchool("Columbia");
    group5.setCourse("COMS4156");
    group5.setMemberIds(new ArrayList<>(List.of("student5")));
    group5.setMaxMembers(4);
    group5.setNumMembers(1);

    TimeSlot group5Time = new TimeSlot();
    group5Time.setDay(DayOfWeek.TUESDAY);
    group5Time.setStartTime(LocalTime.of(10, 0));
    group5Time.setEndTime(LocalTime.of(12, 0));

    group5.setSchedules(new ArrayList<>(List.of(group5Time)));
    group5.setStudyStyles(new ArrayList<>(List.of("independent", "quiet")));
    group5.setMeetingMode("in-person");
    group5.setRecruiting(true);

    List<StudyGroup> candidates = new ArrayList<>();
    candidates.add(group1);
    candidates.add(group2);
    candidates.add(group3);
    candidates.add(group4);
    candidates.add(group5);

    when(studyGroupService.getCandidateGroups("Columbia", "COMS4156", "in-person"))
        .thenReturn(candidates);

    // Act
    List<StudyGroup> result = matchingService.findCompatibleStudyGroups(student);

    // Assert
    assertThat(result.size()).isEqualTo(3);

    assertThat(result.get(0).getName()).isEqualTo("ASE Warriors");

    assertThat(result.get(1).getName()).isEqualTo("Mockito Masters");

    assertThat(result.get(2).getName()).isEqualTo("Spring Boot Squad");
  }
}
