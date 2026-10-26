package com.studymate.controller;

// AI-generated (Claude Code): this test class was written by AI and
// reviewed/accepted by Ziyue Fang. See README "AI Documentation" section.

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.dto.LeaveGroupRequest;
import com.studymate.service.MatchingService;
import com.studymate.service.PreferenceService;
import com.studymate.service.StudentService;
import com.studymate.service.StudyGroupService;
import com.studymate.service.StudyMateApiService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Controller-level tests for POST /api/leave. All collaborators are mocked, so these tests only
 * check the endpoint's decision logic and status codes.
 */
@ExtendWith(MockitoExtension.class)
public class LeaveStudyGroupControllerTest {

  @Mock private PreferenceService preferenceService;
  @Mock private StudyMateApiService studyMateApiService;
  @Mock private StudentService studentService;
  @Mock private MatchingService matchingService;
  @Mock private StudyGroupService studyGroupService;

  @InjectMocks private ApiController apiController;

  private static final String VALID_KEY = "valid-key1";
  private static final String OTHER_STUDENT_KEY = "valid-key2";
  private static final String INVALID_KEY = "invalid-key";
  private static final Long STUDENT_ID = 1L;
  private static final Long GROUP_ID = 1L;

  private Student student;
  private StudyGroup studyGroup;
  private LeaveGroupRequest leaveGroupRequest;

  @BeforeEach
  void setUp() {
    // student who owns VALID_KEY
    student = new Student();
    student.setId(STUDENT_ID);
    student.setStudentId("zf2389");
    student.setName("Ziyue");
    student.setEmail("ziyue@example.com");
    student.setSchool("Columbia");
    student.setMajor("Computer Science");
    student.setYear(3);
    student.setApiKey(VALID_KEY);

    // group that currently contains the student
    studyGroup = new StudyGroup();
    studyGroup.setGroupId(GROUP_ID);
    studyGroup.setName("COMS 4156 Study Group");
    studyGroup.setSchool("Columbia");
    studyGroup.setCourse("COMS4156");
    studyGroup.setMemberIds(new ArrayList<>(List.of("other-student", "zf2389")));
    studyGroup.setMaxMembers(4);
    studyGroup.setNumMembers(2);
    studyGroup.setSchedules(new ArrayList<>());
    studyGroup.setStudyStyles(new ArrayList<>(List.of("focused")));
    studyGroup.setMeetingMode("in-person");
    studyGroup.setRecruiting(true);

    leaveGroupRequest = new LeaveGroupRequest();
    leaveGroupRequest.setStudentId(STUDENT_ID);
    leaveGroupRequest.setGroupId(GROUP_ID);
  }

  @Test
  void leaveStudyGroupWithInvalidApiKeyReturnsUnauthorized() {
    when(studyMateApiService.validateApiKey(INVALID_KEY)).thenReturn(false);

    ResponseEntity<?> response = apiController.leaveStudyGroup(INVALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verifyNoInteractions(studentService, studyGroupService);
  }

  @Test
  void leaveStudyGroupWithNullBodyReturnsBadRequest() {
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(studentService, studyGroupService);
  }

  @Test
  void leaveStudyGroupWithMissingStudentIdReturnsBadRequest() {
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    leaveGroupRequest.setStudentId(null);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(studentService, studyGroupService);
  }

  @Test
  void leaveStudyGroupWithMissingGroupIdReturnsBadRequest() {
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    leaveGroupRequest.setGroupId(null);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(studentService, studyGroupService);
  }

  @Test
  void leaveStudyGroupWithNonExistentStudentReturnsNotFound() {
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(null);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    verify(studyGroupService, never()).leaveGroup(any(), any());
  }

  @Test
  void leaveStudyGroupWithNonExistentGroupReturnsNotFound() {
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(null);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    verify(studyGroupService, never()).leaveGroup(any(), any());
  }

  @Test
  void leaveStudyGroupWithAnotherStudentsKeyReturnsForbidden() {
    // OTHER_STUDENT_KEY is a real key, but it does not belong to student 1
    when(studyMateApiService.validateApiKey(OTHER_STUDENT_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);

    ResponseEntity<?> response =
        apiController.leaveStudyGroup(OTHER_STUDENT_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(studyGroupService, never()).leaveGroup(any(), any());
  }

  @Test
  void leaveStudyGroupWhenNotMemberIsIdempotentAndReturnsOk() {
    // a repeated leave call after the student already left
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);
    when(studyGroupService.isMember(student, studyGroup)).thenReturn(false);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(studyGroup);
    verify(studyGroupService, never()).leaveGroup(any(), any());
  }

  @Test
  void leaveStudyGroupSuccessRemovesMemberAndReturnsOk() {
    StudyGroup updated = new StudyGroup();
    updated.setGroupId(GROUP_ID);
    updated.setMemberIds(new ArrayList<>(List.of("other-student")));
    updated.setNumMembers(1);

    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);
    when(studyGroupService.isMember(student, studyGroup)).thenReturn(true);
    when(studyGroupService.leaveGroup(student, studyGroup)).thenReturn(updated);

    ResponseEntity<?> response = apiController.leaveStudyGroup(VALID_KEY, leaveGroupRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(updated);
    verify(studyGroupService).leaveGroup(student, studyGroup);
  }
}
