package com.studymate.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.studymate.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for API-key validation in {@link StudyMateApiService}. */
@ExtendWith(MockitoExtension.class)
public class StudyMateApiServiceTest {

  @Mock private StudentRepository studentRepository;
  @InjectMocks private StudyMateApiService studyMateApiService;

  @Test
  void validateApiKeyTest() {
    // Arrange
    String apiKey = "valid-key";
    when(studentRepository.existsByApiKey(apiKey)).thenReturn(true);
    // Act
    boolean result = studyMateApiService.validateApiKey(apiKey);
    // Assert
    assertTrue(result);

    // Arrange
    String apiKey2 = "invalid-key";
    when(studentRepository.existsByApiKey(apiKey2)).thenReturn(false);
    // Act
    boolean result2 = studyMateApiService.validateApiKey(apiKey2);

    // Assert
    assertThat(result2).isEqualTo(false);
  }
}
