package com.studymate.repository;

import com.studymate.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for {@link Student} records and API-key lookups. */
public interface StudentRepository extends JpaRepository<Student, Long> {

  boolean existsByApiKey(String apiKey);

  boolean existsByStudentId(String studentId);
}
