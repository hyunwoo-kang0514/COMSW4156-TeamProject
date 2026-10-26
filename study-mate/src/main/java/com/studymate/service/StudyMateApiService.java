package com.studymate.service;

import com.studymate.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Validates the X-API-KEY header against registered students. */
@Service
public class StudyMateApiService {

  private final StudentRepository studentRepository;

  @Autowired
  public StudyMateApiService(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
  }

  /**
   * Validate api key exist.
   *
   * @param apiKey api key
   * @return if api key exist return true, otherwise return false
   */
  public boolean validateApiKey(final String apiKey) {
    return studentRepository.existsByApiKey(apiKey);
  }
}
