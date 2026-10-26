package com.studymate.controller;

// AI-generated (Claude Code): this entire test class was written by AI and
// reviewed/accepted by Hyunwoo Kang. See README "AI Documentation" section.

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.dto.JoinGroupRequest;
import com.studymate.dto.RegisterStudentRequest;
import com.studymate.dto.RegisterStudentResponse;
import com.studymate.service.MatchingService;
import com.studymate.service.PreferenceService;
import com.studymate.service.StudentService;
import com.studymate.service.StudyGroupService;
import com.studymate.service.StudyMateApiService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Unit tests for the find and join endpoints in {@link ApiController}. */
@ExtendWith(MockitoExtension.class)
public class ApiControllerTest {

  @Mock private StudyMateApiService studyMateApiService;
  @Mock private StudentService studentService;
  @Mock private MatchingService matchingService;
  @Mock private StudyGroupService studyGroupService;

  @Mock private PreferenceService preferenceService;

  @InjectMocks private ApiController apiController;

  private static final String VALID_KEY = "valid-key1";
  private static final String INVALID_KEY = "invalid-key";
  private static final Long STUDENT_ID = 1L;
  private static final Long GROUP_ID = 1L;

  private Student student;
  private Preference preference;
  private StudyGroup studyGroup;
  private JoinGroupRequest joinGroupRequest;

  @BeforeEach
  void setUp() {
    // student
    student = new Student();
    student.setId(STUDENT_ID);
    student.setStudentId("hk3382");
    student.setName("Harry");
    student.setEmail("harry@example.com");
    student.setSchool("Columbia");
    student.setMajor("Computer Science");
    student.setYear(4);
    student.setApiKey(VALID_KEY);

    // preference
    preference = new Preference();
    preference.setCourse("COMS4156");
    preference.setMeetingMode("in-person");
    preference.setPreferredGroupSize(4);
    preference.setStudyStyles(new ArrayList<>(List.of("collaborative", "focused")));
    preference.setAvailableTimes(new ArrayList<>());

    // study group
    studyGroup = new StudyGroup();
    studyGroup.setGroupId(GROUP_ID);
    studyGroup.setName("COMS 4156 Study Group");
    studyGroup.setSchool("Columbia");
    studyGroup.setCourse("COMS4156");
    studyGroup.setMemberIds(new ArrayList<>(List.of("other-student")));
    studyGroup.setMaxMembers(4);
    studyGroup.setNumMembers(1);
    studyGroup.setSchedules(new ArrayList<>());
    studyGroup.setStudyStyles(new ArrayList<>(List.of("focused")));
    studyGroup.setMeetingMode("in-person");
    studyGroup.setRecruiting(true);

    // join request body
    joinGroupRequest = new JoinGroupRequest();
    joinGroupRequest.setStudentId(STUDENT_ID);
    joinGroupRequest.setGroupId(GROUP_ID);
  }

  // ---------- GET /api/find : findCompatibleStudyGroups ----------

  @Test
  void findCompatibleStudyGroupsWithInvalidApiKeyReturnsUnauthorized() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(INVALID_KEY)).thenReturn(false);

    // Act
    ResponseEntity<?> response = apiController.findCompatibleStudyGroups(INVALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void findCompatibleStudyGroupsWithNonExistentStudentReturnsNotFound() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(null);

    // Act
    ResponseEntity<?> response = apiController.findCompatibleStudyGroups(VALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void findCompatibleStudyGroupsWithoutPreferenceReturnsBadRequest() throws Exception {
    // Arrange
    student.setPreference(null);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);

    // Act
    ResponseEntity<?> response = apiController.findCompatibleStudyGroups(VALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void findCompatibleStudyGroupsSuccessReturnsRankedMatches() throws Exception {
    // Arrange
    student.setPreference(preference);
    List<StudyGroup> matches = List.of(studyGroup);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(matchingService.findCompatibleStudyGroups(student)).thenReturn(matches);

    // Act
    ResponseEntity<?> response = apiController.findCompatibleStudyGroups(VALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(matches);
  }

  @Test
  void findCompatibleStudyGroupsWithNoCompatibleGroupsReturnsEmptyList() throws Exception {
    // Arrange
    student.setPreference(preference);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(matchingService.findCompatibleStudyGroups(student)).thenReturn(Collections.emptyList());

    // Act
    ResponseEntity<?> response = apiController.findCompatibleStudyGroups(VALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) response.getBody()).isEmpty();
  }

  // ---------- GET /api/join : joinStudyGroup ----------

  @Test
  void joinStudyGroupWithInvalidApiKeyReturnsUnauthorized() {
    // Arrange
    when(studyMateApiService.validateApiKey(INVALID_KEY)).thenReturn(false);

    // Act
    ResponseEntity<?> response = apiController.joinStudyGroup(INVALID_KEY, joinGroupRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(studyGroupService, never()).joinGroup(student, studyGroup);
  }

  @Test
  void joinStudyGroupWithNonExistentStudentReturnsBadRequest() {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(null);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);

    // Act
    ResponseEntity<?> response = apiController.joinStudyGroup(VALID_KEY, joinGroupRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(studyGroupService, never()).joinGroup(student, studyGroup);
  }

  @Test
  void joinStudyGroupWithNonExistentGroupReturnsBadRequest() {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(null);

    // Act
    ResponseEntity<?> response = apiController.joinStudyGroup(VALID_KEY, joinGroupRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(studyGroupService, never()).joinGroup(student, studyGroup);
  }

  @Test
  void joinStudyGroupWhenAlreadyMemberIsIdempotentAndReturnsOk() {
    // Arrange: duplicate join request for a student who is already in the group
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);
    when(studyGroupService.isMember(student, studyGroup)).thenReturn(true);

    // Act
    ResponseEntity<?> response = apiController.joinStudyGroup(VALID_KEY, joinGroupRequest);

    // Assert: success with the current group record, no duplicate membership added
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(studyGroup);
    verify(studyGroupService, never()).joinGroup(student, studyGroup);
  }

  @Test
  void joinStudyGroupWhenGroupFullReturnsBadRequest() {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);
    when(studyGroupService.isMember(student, studyGroup)).thenReturn(false);
    when(studyGroupService.canJoin(studyGroup)).thenReturn(false);

    // Act
    ResponseEntity<?> response = apiController.joinStudyGroup(VALID_KEY, joinGroupRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(studyGroupService, never()).joinGroup(student, studyGroup);
  }

  @Test
  void joinStudyGroupSuccessAddsMemberAndReturnsOk() {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(studentService.findStudentById(STUDENT_ID)).thenReturn(student);
    when(studyGroupService.findStudyGroupById(GROUP_ID)).thenReturn(studyGroup);
    when(studyGroupService.isMember(student, studyGroup)).thenReturn(false);
    when(studyGroupService.canJoin(studyGroup)).thenReturn(true);
    when(studyGroupService.joinGroup(student, studyGroup)).thenReturn(studyGroup);

    // Act
    ResponseEntity<?> response = apiController.joinStudyGroup(VALID_KEY, joinGroupRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(studyGroup);
    verify(studyGroupService).joinGroup(student, studyGroup);
  }

  // Preference API Testing

  // ---------- POST /api/ : addStudentStudyPreferences ----------

  @Test
  void addStudentStudyPreferencesWithInvalidApiKeyReturnsUnauthorized() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(INVALID_KEY)).thenReturn(false);

    // Act
    ResponseEntity<Preference> response =
        apiController.addStudentStudyPreferences(INVALID_KEY, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(preferenceService, never()).addStudentStudyPreferences(any());
  }

  @Test
  void addStudentStudyPreferencesSuccessReturnsCreated() throws Exception {
    // Arrange
    preference.setId(STUDENT_ID);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.addStudentStudyPreferences(preference)).thenReturn(preference);

    // Act
    ResponseEntity<Preference> response =
        apiController.addStudentStudyPreferences(VALID_KEY, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo(preference);
    verify(preferenceService).addStudentStudyPreferences(preference);
  }

  @Test
  void addStudentStudyPreferencesWithNonExistentStudentReturnsNotFound() throws Exception {
    // Arrange
    preference.setId(STUDENT_ID);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.addStudentStudyPreferences(preference))
        .thenThrow(new NoSuchElementException("Student not found for studentId " + STUDENT_ID));

    // Act
    ResponseEntity<Preference> response =
        apiController.addStudentStudyPreferences(VALID_KEY, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void addStudentStudyPreferencesWithoutIdReturnsBadRequest() throws Exception {
    // Arrange
    preference.setId(null);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.addStudentStudyPreferences(preference))
        .thenThrow(new IllegalArgumentException("id is required"));

    // Act
    ResponseEntity<Preference> response =
        apiController.addStudentStudyPreferences(VALID_KEY, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  // ---------- PUT /api/{id} : updateStudentStudyPreferences ----------

  @Test
  void updateStudentStudyPreferencesWithInvalidApiKeyReturnsUnauthorized() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(INVALID_KEY)).thenReturn(false);

    // Act
    ResponseEntity<Preference> response =
        apiController.updateStudentStudyPreferences(INVALID_KEY, STUDENT_ID, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(preferenceService, never()).updateStudentStudyPreferences(anyLong(), any());
  }

  @Test
  void updateStudentStudyPreferencesSuccessReturnsOk() throws Exception {
    // Arrange
    Preference updated = new Preference();
    updated.setId(STUDENT_ID);
    updated.setCourse("COMS4111");
    updated.setMeetingMode("online");
    updated.setPreferredGroupSize(3);
    updated.setStudyStyles(new ArrayList<>(List.of("quiet")));
    updated.setAvailableTimes(new ArrayList<>());
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.updateStudentStudyPreferences(STUDENT_ID, preference))
        .thenReturn(updated);

    // Act
    ResponseEntity<Preference> response =
        apiController.updateStudentStudyPreferences(VALID_KEY, STUDENT_ID, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(updated);
    verify(preferenceService).updateStudentStudyPreferences(STUDENT_ID, preference);
  }

  @Test
  void updateStudentStudyPreferencesWithNonExistentPreferenceReturnsNotFound() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.updateStudentStudyPreferences(STUDENT_ID, preference))
        .thenThrow(new NoSuchElementException("Preference not found for studentId " + STUDENT_ID));

    // Act
    ResponseEntity<Preference> response =
        apiController.updateStudentStudyPreferences(VALID_KEY, STUDENT_ID, preference);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ---------- GET /api/{id} : getStudentStudyPreferences ----------

  @Test
  void getStudentStudyPreferencesWithInvalidApiKeyReturnsUnauthorized() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(INVALID_KEY)).thenReturn(false);

    // Act
    ResponseEntity<Preference> response =
        apiController.getStudentStudyPreferences(INVALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(preferenceService, never()).getStudentStudyPreferences(anyLong());
  }

  @Test
  void getStudentStudyPreferencesSuccessReturnsOk() throws Exception {
    // Arrange
    preference.setId(STUDENT_ID);
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.getStudentStudyPreferences(STUDENT_ID)).thenReturn(preference);

    // Act
    ResponseEntity<Preference> response =
        apiController.getStudentStudyPreferences(VALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(preference);
    verify(preferenceService).getStudentStudyPreferences(STUDENT_ID);
  }

  @Test
  void getStudentStudyPreferencesWithNonExistentPreferenceReturnsNotFound() throws Exception {
    // Arrange
    when(studyMateApiService.validateApiKey(VALID_KEY)).thenReturn(true);
    when(preferenceService.getStudentStudyPreferences(STUDENT_ID))
        .thenThrow(new NoSuchElementException("Preference not found for studentId " + STUDENT_ID));

    // Act
    ResponseEntity<Preference> response =
        apiController.getStudentStudyPreferences(VALID_KEY, STUDENT_ID);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ---------- POST /api/students : registerStudent ----------

  @Test
  void registerStudentSuccessReturnsCreated() {
    // Arrange
    RegisterStudentRequest request = new RegisterStudentRequest();
    request.setStudentId("hc3689");
    request.setName("Kevin");
    request.setEmail("kevin@example.com");
    request.setSchool("Columbia");
    request.setMajor("Computer Science");
    request.setYear(1);

    when(studentService.registerStudent(any(Student.class))).thenReturn(student);

    // Act
    ResponseEntity<?> response = apiController.registerStudent(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isInstanceOf(RegisterStudentResponse.class);

    RegisterStudentResponse body = (RegisterStudentResponse) response.getBody();

    assertThat(body.getStudentId()).isEqualTo(student.getStudentId());
    assertThat(body.getName()).isEqualTo(student.getName());
    assertThat(body.getApiKey()).isEqualTo(student.getApiKey());
    verify(studentService).registerStudent(any(Student.class));
  }

  @Test
  void registerStudentWithInvalidInformationReturnsBadRequest() {
    // Arrange
    RegisterStudentRequest request = new RegisterStudentRequest();
    request.setStudentId("hc3689");

    when(studentService.registerStudent(any(Student.class)))
        .thenThrow(new IllegalArgumentException("Invalid student information"));

    // Act
    ResponseEntity<?> response = apiController.registerStudent(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void registerStudentWithDuplicateIdReturnsConflict() {
    // Arrange
    RegisterStudentRequest request = new RegisterStudentRequest();
    request.setStudentId("hc3689");

    when(studentService.registerStudent(any(Student.class)))
        .thenThrow(new IllegalStateException("Student ID already registered"));

    // Act
    ResponseEntity<?> response = apiController.registerStudent(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }
}
