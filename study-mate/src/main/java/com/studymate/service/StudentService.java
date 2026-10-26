package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.repository.StudentRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Looks up and registers students. */
@Service
public class StudentService {

  private final StudentRepository studentRepository;

  @Autowired
  public StudentService(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
  }

  /**
   * Find student by student id.
   *
   * @param studentId id of the student
   * @return corresponding Student object, return null if DNE
   */
  public Student findStudentById(Long studentId) {
    return studentRepository.findById(studentId).orElse(null);
  }

  /**
   * Register a new student.
   *
   * @param student student information
   * @return registered student
   */
  public Student registerStudent(Student student) {

    if (student == null
        || student.getStudentId() == null
        || student.getStudentId().isBlank()
        || student.getName() == null
        || student.getName().isBlank()
        || student.getEmail() == null
        || student.getEmail().isBlank()
        || student.getSchool() == null
        || student.getSchool().isBlank()
        || student.getMajor() == null
        || student.getMajor().isBlank()
        || student.getYear() <= 0) {
      throw new IllegalArgumentException("Invalid student information");
    }

    if (studentRepository.existsByStudentId(student.getStudentId())) {
      throw new IllegalStateException("Student ID already registered");
    }

    String apiKey = UUID.randomUUID().toString();

    while (studentRepository.existsByApiKey(apiKey)) {
      apiKey = UUID.randomUUID().toString();
    }

    student.setApiKey(apiKey);
    return studentRepository.save(student);
  }
}
