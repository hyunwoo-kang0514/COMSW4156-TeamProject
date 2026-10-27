package com.studymate.request;

// AI-generated with OpenAI Codex; see README "AI Documentation".

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Tests the creation request's JSON contract without starting the application or database. */
class CreateStudyGroupRequestTests {

  // Rejection must work even when the application ignores unknown fields in other requests.
  private final JsonMapper mapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  @Test
  void readsCallerProvidedFieldsAndWeeklySchedules() {
    String json =
        """
        {
          "creatorStudentId": 7,
          "name": "Software Engineering Study Group",
          "course": "COMS W4156",
          "maxMembers": 4,
          "schedules": [
            {"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}
          ],
          "studyStyles": ["DISCUSSION", "PRACTICE"],
          "meetingMode": "ONLINE"
        }
        """;

    CreateStudyGroupRequest request = mapper.readValue(json, CreateStudyGroupRequest.class);

    assertEquals(7L, request.getCreatorStudentId());
    assertEquals("Software Engineering Study Group", request.getName());
    assertEquals("COMS W4156", request.getCourse());
    assertEquals(4, request.getMaxMembers());
    assertEquals(List.of("DISCUSSION", "PRACTICE"), request.getStudyStyles());
    assertEquals("ONLINE", request.getMeetingMode());
    assertEquals(1, request.getSchedules().size());
    assertEquals(DayOfWeek.MONDAY, request.getSchedules().get(0).getDay());
    assertEquals(LocalTime.of(14, 0), request.getSchedules().get(0).getStartTime());
    assertEquals(LocalTime.of(16, 0), request.getSchedules().get(0).getEndTime());
  }

  @ParameterizedTest
  @MethodSource("unrecognizedFields")
  void rejectsServerControlledFieldsAndTypos(String fieldName, String jsonValue) {
    String json = "{\"" + fieldName + "\":" + jsonValue + "}";

    DatabindException exception =
        assertThrows(
            DatabindException.class, () -> mapper.readValue(json, CreateStudyGroupRequest.class));

    assertTrue(exception.getMessage().contains(fieldName));
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
        "studySytles",
        "unexpectedField"
      })
  void rejectsUnrecognizedFieldsEvenWhenTheirValueIsNull(String fieldName) {
    String json = "{\"" + fieldName + "\":null}";

    assertThrows(
        DatabindException.class, () -> mapper.readValue(json, CreateStudyGroupRequest.class));
  }

  private static Stream<Arguments> unrecognizedFields() {
    return Stream.of(
        Arguments.of("groupId", "99"),
        Arguments.of("school", "\"Another School\""),
        Arguments.of("memberIds", "[\"another-student\"]"),
        Arguments.of("numMembers", "3"),
        Arguments.of("isRecruiting", "false"),
        Arguments.of("recruiting", "false"),
        Arguments.of("studySytles", "[\"DISCUSSION\"]"),
        Arguments.of("unexpectedField", "{}"));
  }
}
