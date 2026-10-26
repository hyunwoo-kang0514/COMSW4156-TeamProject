package com.studymate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studymate.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for POST /api/students against the in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
public class RegisterStudentIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private StudentRepository studentRepository;

  @Test
  void registerStudentSuccess() throws Exception {
    String requestBody =
        """
                {
                    "studentId": "test123",
                    "name": "Test Student",
                    "email": "test@example.com",
                    "school": "Columbia",
                    "major": "Computer Science",
                    "year": 1
                }
                """;

    mockMvc
        .perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.studentId").value("test123"))
        .andExpect(jsonPath("$.apiKey").isNotEmpty());
  }

  @Test
  void registerStudentInvalidInput() throws Exception {
    String requestBody =
        """
            {
                "studentId": "invalid123",
                "name": "Test Student",
                "email": "",
                "school": "Columbia",
                "major": "Computer Science",
                "year": 1
            }
            """;

    mockMvc
        .perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerStudentDuplicateId() throws Exception {
    String requestBody =
        """
            {
                "studentId": "duplicate123",
                "name": "Test Student",
                "email": "test@example.com",
                "school": "Columbia",
                "major": "Computer Science",
                "year": 1
            }
            """;

    mockMvc
        .perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isConflict());
  }
}
