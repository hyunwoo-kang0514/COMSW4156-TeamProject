package com.studymate.api;

// AI-generated with OpenAI Codex; see README "AI Documentation".

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import com.studymate.repository.PreferenceRepository;
import com.studymate.repository.StudentRepository;
import com.studymate.repository.StudyGroupRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api")
class LiveStudyMateApiTests {

  private static final String API_KEY = "demo-key";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @LocalServerPort private int port;
  @Autowired private StudentRepository students;
  @Autowired private PreferenceRepository preferences;
  @Autowired private StudyGroupRepository groups;
  @Autowired private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactions;
  private Student creator;

  @BeforeEach
  void seedDatabase() {
    transactions = new TransactionTemplate(transactionManager);
    creator =
        transactions.execute(
            status -> {
              groups.deleteAll();
              students.findAll().forEach(student -> student.setPreference(null));
              preferences.deleteAll();
              preferences.flush();
              students.deleteAll();
              students.flush();
              Student student = new Student();
              student.setStudentId("api-creator");
              student.setName("API Test Student");
              student.setSchool("Columbia");
              student.setApiKey(API_KEY);
              return students.saveAndFlush(student);
            });
  }

  @Test
  void healthRespondsOverHttp() throws Exception {
    HttpResponse<String> response = send("GET", "/api/health", null, null, null);
    assertEquals(200, response.statusCode());
    assertEquals("StudyMate API is running", response.body());
  }

  @Test
  void creationCommitsGroupMembershipSchedulesAndStyles() throws Exception {
    HttpResponse<String> response = create(validGroup());
    assertEquals(200, response.statusCode(), response.body());
    assertTrue(
        response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    JsonNode body = JSON.readTree(response.body());
    long groupId = body.get("groupId").asLong();
    assertTrue(groupId > 0);
    assertEquals("Columbia", body.get("school").asString());
    assertEquals("api-creator", body.get("memberIds").get(0).asString());
    assertEquals(1, body.get("numMembers").asInt());
    assertTrue(body.get("recruiting").asBoolean());

    transactions.executeWithoutResult(
        status -> {
          StudyGroup persisted = groups.findById(groupId).orElseThrow();
          assertEquals("Study Buddies", persisted.getName());
          assertEquals("COMS W4156", persisted.getCourse());
          assertEquals("Columbia", persisted.getSchool());
          assertEquals(List.of("api-creator"), persisted.getMemberIds());
          assertEquals(1, persisted.getNumMembers());
          assertEquals(4, persisted.getMaxMembers());
          assertTrue(persisted.isRecruiting());
          assertEquals("ONLINE", persisted.getMeetingMode());
          assertEquals(List.of("DISCUSSION", "PRACTICE"), persisted.getStudyStyles());
          assertEquals(1, persisted.getSchedules().size());
          assertEquals(DayOfWeek.MONDAY, persisted.getSchedules().get(0).getDay());
          assertEquals(LocalTime.of(14, 0), persisted.getSchedules().get(0).getStartTime());
          assertEquals(LocalTime.of(16, 0), persisted.getSchedules().get(0).getEndTime());
        });
  }

  @Test
  void repeatedHttpRequestsPersistSeparateGroups() throws Exception {
    HttpResponse<String> first = create(validGroup());
    HttpResponse<String> second = create(validGroup());
    assertEquals(200, first.statusCode(), first.body());
    assertEquals(200, second.statusCode(), second.body());
    assertNotEquals(
        JSON.readTree(first.body()).get("groupId").asLong(),
        JSON.readTree(second.body()).get("groupId").asLong());
    assertEquals(2, groups.count());
  }

  @Test
  void missingKeyRejectsCreationWithoutSaving() throws Exception {
    HttpResponse<String> response =
        send("POST", "/api/groups", validGroup().toString(), null, "application/json");
    assertEquals(401, response.statusCode(), response.body());
    assertEquals(0, groups.count());
  }

  @Test
  void blankKeyRejectsCreationWithoutSaving() throws Exception {
    HttpResponse<String> response =
        send("POST", "/api/groups", validGroup().toString(), "", "application/json");
    assertEquals(401, response.statusCode(), response.body());
    assertEquals(0, groups.count());
  }

  @Test
  void unregisteredKeyRejectsCreationWithoutSaving() throws Exception {
    HttpResponse<String> response =
        send("POST", "/api/groups", validGroup().toString(), "unknown-key", "application/json");
    assertEquals(401, response.statusCode(), response.body());
    assertEquals(0, groups.count());
  }

  @Test
  void unknownCreatorReturnsNotFoundWithoutSaving() throws Exception {
    ObjectNode request = validGroup().put("creatorStudentId", Long.MAX_VALUE);
    HttpResponse<String> response = create(request);
    assertEquals(404, response.statusCode(), response.body());
    assertTrue(
        JSON.readTree(response.body()).get("error").asString().contains("Student not found"));
    assertEquals(0, groups.count());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"creatorStudentId\":null}",
        "{\"name\":\" \"}",
        "{\"course\":null}",
        "{\"maxMembers\":1}",
        "{\"maxMembers\":null}",
        "{\"meetingMode\":\"ANY\"}",
        "{\"studyStyles\":[]}",
        "{\"studyStyles\":[\"DISCUSSION\",\" DISCUSSION \"]}",
        "{\"schedules\":[]}",
        "{\"schedules\":[{\"day\":\"MONDAY\",\"startTime\":\"16:00\",\"endTime\":\"14:00\"}]}"
      })
  void invalidGroupReturnsBadRequestAndLeavesDatabaseUnchanged(String patch) throws Exception {
    ObjectNode request = validGroup();
    request.setAll((ObjectNode) JSON.readTree(patch));
    HttpResponse<String> response = create(request);
    assertEquals(400, response.statusCode(), response.body());
    assertTrue(JSON.readTree(response.body()).has("error"));
    assertEquals(0, groups.count());
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
  void forbiddenFieldsAreRejectedBeforeAnyGroupIsSaved(String field) throws Exception {
    ObjectNode request = validGroup();
    request.putNull(field);
    HttpResponse<String> response = create(request);
    assertEquals(400, response.statusCode(), response.body());
    assertEquals(0, groups.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "null", "{", "[]", "{\"creatorStudentId\":\"invalid\"}"})
  void malformedJsonReturnsBadRequestWithoutSaving(String body) throws Exception {
    HttpResponse<String> response = send("POST", "/api/groups", body, API_KEY, "application/json");
    assertEquals(400, response.statusCode(), response.body());
    assertEquals(0, groups.count());
  }

  @Test
  void plainTextReturnsUnsupportedMediaTypeWithoutSaving() throws Exception {
    HttpResponse<String> response =
        send("POST", "/api/groups", validGroup().toString(), API_KEY, "text/plain");
    assertEquals(415, response.statusCode(), response.body());
    assertEquals(0, groups.count());
  }

  @Test
  void preferenceCanBeReadAndUpdatedOverHttp() throws Exception {
    seedPreference();
    String path = "/api/" + creator.getId();
    HttpResponse<String> initial = send("GET", path, null, API_KEY, null);
    assertEquals(200, initial.statusCode(), initial.body());
    assertEquals("COMS W4156", JSON.readTree(initial.body()).get("course").asString());

    ObjectNode update = validGroup();
    update.remove(List.of("creatorStudentId", "name", "maxMembers", "schedules"));
    update.put("preferredGroupSize", 5);
    update.set(
        "availableTimes",
        JSON.readTree("[{\"day\":\"TUESDAY\",\"startTime\":\"10:00\",\"endTime\":\"12:00\"}]"));
    HttpResponse<String> changed =
        send("PUT", path, update.toString(), API_KEY, "application/json");
    assertEquals(200, changed.statusCode(), changed.body());
    HttpResponse<String> reread = send("GET", path, null, API_KEY, null);
    assertEquals(200, reread.statusCode(), reread.body());
    assertEquals(5, JSON.readTree(reread.body()).get("preferredGroupSize").asInt());
    transactions.executeWithoutResult(
        status -> {
          Preference persisted = preferences.findById(creator.getId()).orElseThrow();
          assertEquals(5, persisted.getPreferredGroupSize());
          assertEquals(DayOfWeek.TUESDAY, persisted.getAvailableTimes().get(0).getDay());
        });
  }

  // AI-generated (Claude Code), reviewed by Ziyue Fang. See README "AI Documentation".
  @Test
  void preferenceCanBeCreatedOverHttp() throws Exception {
    String body =
        """
        {
          "id": %d,
          "course": "COMS W4156",
          "meetingMode": "ONLINE",
          "preferredGroupSize": 4,
          "studyStyles": ["DISCUSSION"],
          "availableTimes": [{"day": "MONDAY", "startTime": "14:00", "endTime": "16:00"}]
        }
        """
            .formatted(creator.getId());
    HttpResponse<String> created = send("POST", "/api/", body, API_KEY, "application/json");
    assertEquals(201, created.statusCode(), created.body());
    transactions.executeWithoutResult(
        status -> {
          Preference persisted = preferences.findById(creator.getId()).orElseThrow();
          assertEquals("COMS W4156", persisted.getCourse());
          assertEquals(List.of("DISCUSSION"), persisted.getStudyStyles());
        });
  }

  // AI-generated (Claude Code), reviewed by Ziyue Fang. See README "AI Documentation".
  @Test
  void preferenceEndpointsReturnNotFoundForUnknownStudents() throws Exception {
    long missingId = creator.getId() + 1000;
    String body =
        "{\"id\": %d, \"course\": \"COMS W4156\", \"preferredGroupSize\": 4}".formatted(missingId);
    HttpResponse<String> created = send("POST", "/api/", body, API_KEY, "application/json");
    assertEquals(404, created.statusCode(), created.body());
    HttpResponse<String> read = send("GET", "/api/" + missingId, null, API_KEY, null);
    assertEquals(404, read.statusCode(), read.body());
    HttpResponse<String> updated =
        send("PUT", "/api/" + missingId, body, API_KEY, "application/json");
    assertEquals(404, updated.statusCode(), updated.body());
    assertEquals(0, preferences.count());
  }

  @Test
  void createdGroupIsReturnedByMatchingEndpoint() throws Exception {
    seedPreference();
    HttpResponse<String> created = create(validGroup());
    assertEquals(200, created.statusCode(), created.body());
    long groupId = JSON.readTree(created.body()).get("groupId").asLong();

    HttpResponse<String> matches =
        send("GET", "/api/find?studentId=" + creator.getId(), null, API_KEY, null);
    assertEquals(200, matches.statusCode(), matches.body());
    JsonNode result = JSON.readTree(matches.body());
    assertEquals(1, result.size());
    assertEquals(groupId, result.get(0).get("groupId").asLong());
  }

  @Test
  void matchingRejectsUnknownStudentsAndStudentsWithoutPreferences() throws Exception {
    HttpResponse<String> missing =
        send("GET", "/api/find?studentId=" + Long.MAX_VALUE, null, API_KEY, null);
    assertEquals(404, missing.statusCode(), missing.body());
    HttpResponse<String> noPreference =
        send("GET", "/api/find?studentId=" + creator.getId(), null, API_KEY, null);
    assertEquals(400, noPreference.statusCode(), noPreference.body());
  }

  @Test
  void matchingReturnsEmptyListWhenNoGroupsExist() throws Exception {
    seedPreference();
    HttpResponse<String> response =
        send("GET", "/api/find?studentId=" + creator.getId(), null, API_KEY, null);
    assertEquals(200, response.statusCode(), response.body());
    assertEquals(0, JSON.readTree(response.body()).size());
  }

  @Test
  void matchingReturnsOnlyTopThreeGroupsInScoreOrder() throws Exception {
    seedPreference();
    final long best = createWithPatch("{}");
    final long second =
        createWithPatch(
            """
        {"schedules":[{"day":"MONDAY","startTime":"14:00","endTime":"15:00"}]}
        """);
    final long third =
        createWithPatch(
            """
        {"maxMembers":5,"studyStyles":["DISCUSSION"],
         "schedules":[{"day":"MONDAY","startTime":"14:00","endTime":"15:00"}]}
        """);
    createWithPatch(
        """
        {"maxMembers":8,"studyStyles":["OTHER"],
         "schedules":[{"day":"MONDAY","startTime":"14:00","endTime":"14:30"}]}
        """);
    createWithPatch(
        """
        {"maxMembers":8,"studyStyles":["OTHER"],
         "schedules":[{"day":"TUESDAY","startTime":"14:00","endTime":"16:00"}]}
        """);

    HttpResponse<String> response =
        send("GET", "/api/find?studentId=" + creator.getId(), null, API_KEY, null);
    assertEquals(200, response.statusCode(), response.body());
    JsonNode result = JSON.readTree(response.body());
    assertEquals(3, result.size());
    assertEquals(best, result.get(0).get("groupId").asLong());
    assertEquals(second, result.get(1).get("groupId").asLong());
    assertEquals(third, result.get(2).get("groupId").asLong());
  }

  @Test
  void matchingExcludesDifferentCoursesModesSchoolsClosedAndFullGroups() throws Exception {
    seedPreference();
    createWithPatch("{\"course\":\"OTHER\"}");
    createWithPatch("{\"meetingMode\":\"HYBRID\"}");
    long otherSchool = createWithPatch("{}");
    long closed = createWithPatch("{}");
    long full = createWithPatch("{\"maxMembers\":2}");
    transactions.executeWithoutResult(
        status -> {
          groups.findById(otherSchool).orElseThrow().setSchool("Other School");
          groups.findById(closed).orElseThrow().setRecruiting(false);
          StudyGroup fullGroup = groups.findById(full).orElseThrow();
          fullGroup.getMemberIds().add("existing-member");
          fullGroup.setNumMembers(2);
        });

    HttpResponse<String> response =
        send("GET", "/api/find?studentId=" + creator.getId(), null, API_KEY, null);
    assertEquals(200, response.statusCode(), response.body());
    assertEquals(0, JSON.readTree(response.body()).size());
  }

  /** Creates a persisted candidate with selected values overridden for ranking and filter tests. */
  private long createWithPatch(String patch) throws Exception {
    ObjectNode request = validGroup();
    request.setAll((ObjectNode) JSON.readTree(patch));
    HttpResponse<String> response = create(request);
    assertEquals(200, response.statusCode(), response.body());
    return JSON.readTree(response.body()).get("groupId").asLong();
  }

  /** Builds valid input using the database-generated ID rather than assuming a fixed fixture ID. */
  private ObjectNode validGroup() {
    return (ObjectNode)
        JSON.readTree(
            """
        {
          "creatorStudentId": %d,
          "name": "Study Buddies",
          "course": "COMS W4156",
          "maxMembers": 4,
          "schedules": [{"day":"MONDAY","startTime":"14:00","endTime":"16:00"}],
          "studyStyles": ["DISCUSSION", "PRACTICE"],
          "meetingMode": "ONLINE"
        }
        """
                .formatted(creator.getId()));
  }

  /** Commits a prerequisite preference because the preference-creation API is incomplete. */
  private void seedPreference() {
    transactions.executeWithoutResult(
        status -> {
          Preference preference = new Preference();
          preference.setStudent(students.findById(creator.getId()).orElseThrow());
          preference.setCourse("COMS W4156");
          preference.setPreferredGroupSize(4);
          preference.setMeetingMode("ONLINE");
          preference.setStudyStyles(List.of("DISCUSSION", "PRACTICE"));
          TimeSlot slot = new TimeSlot();
          slot.setDay(DayOfWeek.MONDAY);
          slot.setStartTime(LocalTime.of(14, 0));
          slot.setEndTime(LocalTime.of(16, 0));
          preference.setAvailableTimes(List.of(slot));
          preferences.saveAndFlush(preference);
        });
  }

  /** Submits a group through the real HTTP endpoint. */
  private HttpResponse<String> create(ObjectNode body) throws Exception {
    return send("POST", "/api/groups", body.toString(), API_KEY, "application/json");
  }

  /**
   * Sends a bounded HTTP request to the application's randomly assigned local port.
   *
   * @param method HTTP method
   * @param path endpoint path, including any query parameters
   * @param body request body, or null for no body
   * @param key API key, or null to omit the header
   * @param contentType content type, or null to omit the header
   * @return the server's HTTP response
   * @throws Exception if the request cannot be sent
   */
  protected HttpResponse<String> send(
      String method, String path, String body, String key, String contentType) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
    if (key != null) {
      request.header("X-API-KEY", key);
    }
    if (contentType != null) {
      request.header("Content-Type", contentType);
    }
    return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
