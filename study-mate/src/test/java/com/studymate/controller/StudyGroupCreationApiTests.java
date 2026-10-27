package com.studymate.controller;

// AI-generated with OpenAI Codex; see README "AI Documentation".

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studymate.domain.StudyGroup;
import com.studymate.request.CreateStudyGroupRequest;
import com.studymate.service.MatchingService;
import com.studymate.service.PreferenceService;
import com.studymate.service.StudentService;
import com.studymate.service.StudyGroupService;
import com.studymate.service.StudyMateApiService;
import java.time.DayOfWeek;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StudyGroupCreationApiTests {

  private static final String VALID_JSON =
      """
      {
        "creatorStudentId": 7,
        "name": "Study Buddies",
        "course": "COMS W4156",
        "maxMembers": 4,
        "schedules": [{"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}],
        "studyStyles": ["DISCUSSION"],
        "meetingMode": "ONLINE"
      }
      """;

  private StudyMateApiService apiService;
  private StudyGroupService groupService;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    apiService = mock(StudyMateApiService.class);
    groupService = mock(StudyGroupService.class);
    ApiController controller =
        new ApiController(
            mock(PreferenceService.class),
            apiService,
            mock(StudentService.class),
            mock(MatchingService.class),
            groupService);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
  }

  @Test
  void bindsJsonCallsServiceAndReturnsSavedGroup() throws Exception {
    when(apiService.validateApiKey("valid-key")).thenReturn(true);
    StudyGroup saved = new StudyGroup();
    saved.setGroupId(42L);
    saved.setName("Study Buddies");
    saved.setSchool("Columbia");
    saved.setMemberIds(List.of("abc123"));
    saved.setNumMembers(1);
    saved.setRecruiting(true);
    when(groupService.createStudyGroup(any())).thenReturn(saved);

    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "valid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupId").value(42))
        .andExpect(jsonPath("$.school").value("Columbia"))
        .andExpect(jsonPath("$.memberIds[0]").value("abc123"))
        .andExpect(jsonPath("$.numMembers").value(1))
        .andExpect(jsonPath("$.recruiting").value(true));

    ArgumentCaptor<CreateStudyGroupRequest> captured =
        ArgumentCaptor.forClass(CreateStudyGroupRequest.class);
    verify(groupService).createStudyGroup(captured.capture());
    assertEquals(7L, captured.getValue().getCreatorStudentId());
    assertEquals("Study Buddies", captured.getValue().getName());
    assertEquals("COMS W4156", captured.getValue().getCourse());
    assertEquals(4, captured.getValue().getMaxMembers());
    assertEquals(List.of("DISCUSSION"), captured.getValue().getStudyStyles());
    assertEquals("ONLINE", captured.getValue().getMeetingMode());
    assertEquals(DayOfWeek.MONDAY, captured.getValue().getSchedules().get(0).getDay());
  }

  @Test
  void rejectsMissingKeyBeforeCallingService() throws Exception {
    mvc.perform(post("/api/groups").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(apiService, groupService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void rejectsBlankKeyBeforeCallingValidator(String key) throws Exception {
    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(apiService, groupService);
  }

  @Test
  void rejectsKeyWhenValidatorReturnsFalse() throws Exception {
    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "invalid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON))
        .andExpect(status().isUnauthorized());
    verify(apiService).validateApiKey("invalid-key");
    verifyNoInteractions(groupService);
  }

  @Test
  void mapsMissingStudentToNotFound() throws Exception {
    when(apiService.validateApiKey("valid-key")).thenReturn(true);
    when(groupService.createStudyGroup(any()))
        .thenThrow(new NoSuchElementException("Student not found for ID 7"));

    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "valid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Student not found for ID 7"));
  }

  @Test
  void mapsInvalidGroupToBadRequest() throws Exception {
    when(apiService.validateApiKey("valid-key")).thenReturn(true);
    when(groupService.createStudyGroup(any()))
        .thenThrow(new IllegalArgumentException("maxMembers must be at least 2."));

    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "valid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("maxMembers must be at least 2."));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "null",
        "{",
        "[]",
        "{\"creatorStudentId\":\"not-a-number\"}",
        "{\"schedules\":[{\"day\":\"NOT_A_DAY\"}]}",
        "{\"schedules\":[{\"startTime\":\"not-a-time\"}]}"
      })
  void rejectsMissingOrMalformedJsonBeforeCallingService(String body) throws Exception {
    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "valid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(apiService, groupService);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "groupId",
        "school",
        "memberIds",
        "numMembers",
        "isRecruiting",
        "recruiting",
        "studySytles"
      })
  void rejectsForbiddenOrMisspelledJsonFields(String field) throws Exception {
    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "valid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + field + "\":null}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(groupService);
  }

  @Test
  void rejectsUnsupportedContentType() throws Exception {
    mvc.perform(
            post("/api/groups")
                .header("X-API-KEY", "valid-key")
                .contentType(MediaType.TEXT_PLAIN)
                .content(VALID_JSON))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(groupService);
  }
}
