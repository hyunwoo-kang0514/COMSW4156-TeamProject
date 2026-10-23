package com.studymate.service;
import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import com.studymate.repository.StudyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudyGroupServiceTest {

    @Mock
    private StudyGroupRepository studyGroupRepository;
    @InjectMocks
    private StudyGroupService studyGroupService;

    private Student student;
    private Student student2;
    private StudyGroup studyGroup;
    private StudyGroup studyGroup2;
    private TimeSlot timeSlot;
    private TimeSlot timeSlot2;

    @BeforeEach
    void setUp() {
        List<TimeSlot> timeslots = new ArrayList<>();
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
        List<TimeSlot> timeSlots2 = new ArrayList<>();
        TimeSlot group2TimeSlot1 = new TimeSlot();
        group2TimeSlot1.setDay(DayOfWeek.TUESDAY);
        group2TimeSlot1.setStartTime(LocalTime.of(15, 0));
        group2TimeSlot1.setEndTime(LocalTime.of(17, 0));
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
    void getAllStudyGroups() {
        // Arrange
        List<StudyGroup> allGroups = new ArrayList<>();
        allGroups.add(studyGroup);
        allGroups.add(studyGroup2);
        when(studyGroupRepository.findAll())
                .thenReturn(allGroups);

        // Act
        List<StudyGroup> result = studyGroupService.getAllStudyGroups();

        // Assert
        assertEquals(allGroups, result);

    }

    @Test
    void getCandidateGroups() {
        // Arrange
        List<StudyGroup> candidateGroups = new ArrayList<>();
        candidateGroups.add(studyGroup);
        when(studyGroupRepository.findCandidateGroups(
                "Columbia", "COMS4156", "in-person"))
                .thenReturn(candidateGroups);
        // Act
        List<StudyGroup> result = studyGroupService.getCandidateGroups(
                "Columbia", "COMS4156", "in-person");
        // Assert
        assertEquals(candidateGroups, result);

    }

    @Test
    void findStudyGroupByIdWhenExists() {
        // Arrange
        when(studyGroupRepository.findById(1L))
                .thenReturn(java.util.Optional.of(studyGroup));
        // Act
        StudyGroup result = studyGroupService.findStudyGroupById(1L);
        // Assert
        assertEquals(studyGroup, result);
    }

    @Test
    void findStudyGroupByIdWhenDoesNotExist() {
        // Arrange
        when(studyGroupRepository.findById(999L))
                .thenReturn(java.util.Optional.empty());
        // Act
        StudyGroup result = studyGroupService.findStudyGroupById(999L);
        // Assert
        assertNull(result);
    }


    @Test
    void isMemberWhenStudentIsMember() {
        boolean result = studyGroupService.isMember(student, studyGroup);
        assertThat(result).isTrue();
    }

    @Test
    void isMemberWhenStudentIsNotMember() {
        student.setStudentId("random studentId");
        boolean result = studyGroupService.isMember(student, studyGroup);
        assertThat(result).isFalse();
    }

    @Test
    void canJoinWhenSpaceAvailableAndRecruiting() {
        studyGroup.setRecruiting(true);
        studyGroup.setNumMembers(10);
        studyGroup.setMaxMembers(20);
        boolean result = studyGroupService.canJoin(studyGroup);
        assertThat(result).isEqualTo(true);
    }

    @Test
    void canJoinWhenGroupIsFull() {
        studyGroup.setRecruiting(true);
        studyGroup.setNumMembers(10);
        studyGroup.setMaxMembers(10);
        boolean result = studyGroupService.canJoin(studyGroup);
        assertThat(result).isEqualTo(false);
    }

    @Test
    void canJoinWhenNotRecruiting() {
        studyGroup.setRecruiting(false);
        studyGroup.setNumMembers(5);
        studyGroup.setMaxMembers(10);
        boolean result = studyGroupService.canJoin(studyGroup);
        assertThat(result).isEqualTo(false);
    }

    @Test
    void joinGroup() {
        // Arrange
        Student student3 = new Student();
        student3.setStudentId("new-member");
        int sizeBefore = studyGroup.getNumMembers();

        // Act
        StudyGroup result = studyGroupService.joinGroup(student3, studyGroup);

        // Assert
        assertThat(result.getNumMembers()).isEqualTo(sizeBefore + 1);

        int i = 0;
        for (String id : studyGroup.getMemberIds()) {
            assertThat(id).isEqualTo(studyGroup.getMemberIds().get(i));
            i++;
        }

    }
}
