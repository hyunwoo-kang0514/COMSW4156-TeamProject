package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import com.studymate.repository.StudyGroupRepository;
import com.studymate.request.CreateStudyGroupRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyGroupService {

  private final StudyGroupRepository studyGroupRepository;
  private final StudentService studentService;

  @Autowired
  public StudyGroupService(
      StudyGroupRepository studyGroupRepository, StudentService studentService) {
    this.studyGroupRepository = studyGroupRepository;
    this.studentService = studentService;
  }

  /**
   * Creates and saves a recruiting study group with the creator as its first member.
   *
   * @param request the caller-provided group details and creator's numeric database ID
   * @return the saved group, including its generated group ID
   * @throws IllegalArgumentException if required information or group values are invalid
   * @throws NoSuchElementException if the creator does not exist
   */
  @Transactional
  public StudyGroup createStudyGroup(CreateStudyGroupRequest request) {
    if (request == null || request.getCreatorStudentId() == null
        || request.getCreatorStudentId() <= 0) {
      throw new IllegalArgumentException("A positive creatorStudentId is required.");
    }

    Student creator = studentService.findStudentById(request.getCreatorStudentId());
    if (creator == null) {
      throw new NoSuchElementException(
          "Student not found for ID " + request.getCreatorStudentId());
    }

    requireText(creator.getSchool(), "Creator school");
    requireText(creator.getStudentId(), "Creator student identifier");
    if (request.getMaxMembers() == null || request.getMaxMembers() < 2) {
      throw new IllegalArgumentException("maxMembers must be at least 2.");
    }
    String meetingMode = requireText(request.getMeetingMode(), "meetingMode");
    if (!Set.of("IN_PERSON", "ONLINE", "HYBRID").contains(meetingMode)) {
      throw new IllegalArgumentException(
          "meetingMode must be IN_PERSON, ONLINE, or HYBRID.");
    }

    StudyGroup group = new StudyGroup();
    group.setName(requireText(request.getName(), "name"));
    group.setCourse(requireText(request.getCourse(), "course"));
    group.setSchool(creator.getSchool());
    group.setMaxMembers(request.getMaxMembers());
    group.setSchedules(copyValidSchedules(request.getSchedules()));
    group.setStudyStyles(copyValidStudyStyles(request.getStudyStyles()));
    group.setMeetingMode(meetingMode);
    group.setMemberIds(new ArrayList<>(List.of(creator.getStudentId())));
    group.setNumMembers(group.getMemberIds().size());
    group.setRecruiting(true);
    return studyGroupRepository.save(group);
  }

  /**
   * Validates weekly time slots and copies them without retaining mutable request objects.
   *
   * @param schedules the requested weekly meeting times
   * @return independent, valid time slots
   * @throws IllegalArgumentException if times are missing, reversed, duplicated, or overlapping
   */
  private List<TimeSlot> copyValidSchedules(List<TimeSlot> schedules) {
    if (schedules == null || schedules.isEmpty()) {
      throw new IllegalArgumentException("At least one schedule is required.");
    }
    List<TimeSlot> copies = new ArrayList<>();
    for (TimeSlot schedule : schedules) {
      if (schedule == null || schedule.getDay() == null || schedule.getStartTime() == null
          || schedule.getEndTime() == null
          || !schedule.getStartTime().isBefore(schedule.getEndTime())) {
        throw new IllegalArgumentException(
            "Each schedule requires a day and a start time before its end time.");
      }
      for (TimeSlot existing : copies) {
        if (existing.getDay() == schedule.getDay()
            && schedule.getStartTime().isBefore(existing.getEndTime())
            && existing.getStartTime().isBefore(schedule.getEndTime())) {
          throw new IllegalArgumentException("Schedules on the same day must not overlap.");
        }
      }
      TimeSlot copy = new TimeSlot();
      copy.setDay(schedule.getDay());
      copy.setStartTime(schedule.getStartTime());
      copy.setEndTime(schedule.getEndTime());
      copies.add(copy);
    }
    return copies;
  }

  /**
   * Validates study styles and copies their trimmed values without imposing a fixed vocabulary.
   *
   * @param studyStyles the requested study approaches
   * @return nonblank, unique study styles
   * @throws IllegalArgumentException if styles are missing, blank, or duplicated
   */
  private List<String> copyValidStudyStyles(List<String> studyStyles) {
    if (studyStyles == null || studyStyles.isEmpty()) {
      throw new IllegalArgumentException("At least one study style is required.");
    }
    List<String> copies = new ArrayList<>();
    Set<String> uniqueStyles = new HashSet<>();
    for (String style : studyStyles) {
      String trimmedStyle = requireText(style, "studyStyles entry");
      if (!uniqueStyles.add(trimmedStyle)) {
        throw new IllegalArgumentException("Study styles must not contain duplicates.");
      }
      copies.add(trimmedStyle);
    }
    return copies;
  }

  /**
   * Requires a text value and removes surrounding whitespace.
   *
   * @param value the text to validate
   * @param fieldName the field identified in a validation error
   * @return the trimmed text
   * @throws IllegalArgumentException if the value is null or blank
   */
  private String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required and must not be blank.");
    }
    return value.trim();
  }

  /**
   * Get all groups that exist in the database
   * @return all groups that exist
   */
  public List<StudyGroup> getAllStudyGroups() {
    return studyGroupRepository.findAll();
  }

  /**
   * Get all candidate groups from the database
   */
  public List<StudyGroup> getCandidateGroups(String school, String course, String mode) {
    return studyGroupRepository.findCandidateGroups(school, course, mode);
  }

  /**
   * Find study group by id
   *
   * @param groupId id of the student
   * @return corresponding Student object, return null if DNE
   */
  public StudyGroup findStudyGroupById(Long groupId) {
    return studyGroupRepository.findById(groupId).orElse(null);
  }

  /**
   * Check whether a student is in the group
   * @param student instance of student
   * @param studyGroup instance of study group
   * @return True if already member, false otherwise
   */
  public boolean isMember(Student student, StudyGroup studyGroup) {
    String studentId = student.getStudentId();
    List<String> members = studyGroup.getMemberIds();

    for (String member : members) {
      if (member.equals(studentId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check availability of the group
   * @param studyGroup instance of the study group
   * @return True if can join, false otherwise
   */
  public boolean canJoin(StudyGroup studyGroup) {
    return studyGroup.getNumMembers() < studyGroup.getMaxMembers() && studyGroup.isRecruiting();
  }

  /**
   * Join a student to a group
   * @param student instance of the student
   * @param studyGroup instance of study group
   */
  public void joinGroup(Student student, StudyGroup studyGroup) {
    List<String> newMemberIds = studyGroup.getMemberIds();
    newMemberIds.add(student.getStudentId());
    studyGroup.setMemberIds(newMemberIds);
  }
}
