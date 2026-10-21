package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Find student by student id
     *
     * @param studentId id of the student
     * @return corresponding Student object, return null if DNE
     */
    public Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId).orElse(null);
    }
}