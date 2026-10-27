package com.studymate.api;

// AI-generated with OpenAI Codex: PostgreSQL configuration and live HTTP integration scenarios.

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Runs the shared live API scenarios with PostgreSQL in a disposable schema. */
@ActiveProfiles(value = "postgres-api", inheritProfiles = false)
class PostgresStudyMateApiIt extends LiveStudyMateApiTests {

  private static final String SCHEMA = "api_test_" + UUID.randomUUID().toString().replace("-", "");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static String databaseUrl;
  private static String databaseUser;
  private static String databasePassword;
  private static boolean schemaCreated;

  @Autowired private DataSource dataSource;

  /** Creates an isolated schema before Hibernate starts, with no H2 or production-URL fallback. */
  @DynamicPropertySource
  static void configurePostgres(DynamicPropertyRegistry properties) throws SQLException {
    databaseUrl = requiredEnvironment("POSTGRES_TEST_URL");
    databaseUser = requiredEnvironment("POSTGRES_TEST_USERNAME");
    databasePassword = System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", "");
    if (!databaseUrl.startsWith("jdbc:postgresql:")) {
      throw new IllegalArgumentException("POSTGRES_TEST_URL must be a PostgreSQL JDBC URL.");
    }
    try (Connection connection =
            DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + SCHEMA);
      schemaCreated = true;
    }
    properties.add("spring.datasource.hikari.schema", () -> SCHEMA);
    properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
  }

  @Test
  void connectionUsesPostgreSqlAndTheDisposableSchema() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
      assertEquals(SCHEMA, connection.getSchema());
    }
  }

  @Test
  void registrationJoinAndLeaveCommitOverHttp() throws Exception {
    JsonNode owner = registerStudent("pg-owner");
    JsonNode member = registerStudent("pg-member");
    String groupRequest =
        """
        {"creatorStudentId":%d,"name":"Postgres group","course":"COMS4156",
         "maxMembers":3,"meetingMode":"ONLINE","studyStyles":["DISCUSSION"],
         "schedules":[{"day":"MONDAY","startTime":"14:00","endTime":"16:00"}]}
        """
            .formatted(owner.get("id").asLong());
    String ownerKey = owner.get("apiKey").asString();
    String memberKey = member.get("apiKey").asString();
    HttpResponse<String> created =
        send("POST", "/api/groups", groupRequest, ownerKey, "application/json");
    assertEquals(200, created.statusCode(), created.body());
    long groupId = JSON.readTree(created.body()).get("groupId").asLong();
    String membership =
        "{\"studentId\":" + member.get("id").asLong() + ",\"groupId\":" + groupId + "}";

    for (int attempt = 0; attempt < 2; attempt++) {
      HttpResponse<String> joined =
          send("POST", "/api/join", membership, memberKey, "application/json");
      assertEquals(200, joined.statusCode(), joined.body());
      assertEquals(2, JSON.readTree(joined.body()).get("numMembers").asInt());
    }
    HttpResponse<String> forbidden =
        send("POST", "/api/leave", membership, ownerKey, "application/json");
    assertEquals(403, forbidden.statusCode(), forbidden.body());
    for (int attempt = 0; attempt < 2; attempt++) {
      HttpResponse<String> left =
          send("POST", "/api/leave", membership, memberKey, "application/json");
      assertEquals(200, left.statusCode(), left.body());
      assertEquals(1, JSON.readTree(left.body()).get("numMembers").asInt());
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement count =
            connection.prepareStatement("SELECT num_members FROM study_group WHERE group_id = ?");
        PreparedStatement members =
            connection.prepareStatement(
                "SELECT member_ids FROM study_group_member_ids WHERE study_group_group_id = ?")) {
      count.setLong(1, groupId);
      try (ResultSet result = count.executeQuery()) {
        assertTrue(result.next());
        assertEquals(1, result.getInt(1));
      }
      members.setLong(1, groupId);
      try (ResultSet result = members.executeQuery()) {
        assertTrue(result.next());
        assertEquals("pg-owner", result.getString(1));
        assertFalse(result.next());
      }
    }
  }

  /** Registers prerequisites through the actual HTTP endpoint and returns the generated key. */
  private JsonNode registerStudent(String studentId) throws Exception {
    String request =
        """
        {"studentId":"%s","name":"Postgres Student","email":"student@example.test",
         "school":"Columbia","major":"Computer Science","year":2}
        """
            .formatted(studentId);
    HttpResponse<String> response =
        send("POST", "/api/students", request, null, "application/json");
    assertEquals(201, response.statusCode(), response.body());
    return JSON.readTree(response.body());
  }

  /** Removes only the generated test schema, including when an HTTP scenario fails. */
  @AfterAll
  static void removeTestSchema() throws SQLException {
    if (schemaCreated) {
      try (Connection connection =
              DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
          Statement statement = connection.createStatement()) {
        statement.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
      }
    }
  }

  /** Requires explicit test database settings rather than using the application's DB variables. */
  private static String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required for PostgreSQL API tests.");
    }
    return value;
  }
}
