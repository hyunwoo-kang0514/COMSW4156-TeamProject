package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setStudentId("hc3689");
        student.setName("Kevin");
        student.setEmail("kevin@example.com");
        student.setSchool("Columbia");
        student.setMajor("Computer Science");
        student.setYear(1);
    }

    @Test
    void registerStudentSuccess() {
        when(studentRepository.existsByStudentId("hc3689")).thenReturn(false);
        when(studentRepository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Student result = studentService.registerStudent(student);

        assertEquals("hc3689", result.getStudentId());
        assertNotNull(result.getApiKey());
        assertFalse(result.getApiKey().isBlank());
        verify(studentRepository).save(student);
    }

    @Test
    void registerStudentWithInvalidInformation() {
        student.setEmail("");

        assertThrows(IllegalArgumentException.class,
                () -> studentService.registerStudent(student));

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void registerStudentWithDuplicateId() {
        when(studentRepository.existsByStudentId("hc3689")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> studentService.registerStudent(student));

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void registerStudentWithMissingName() {
        student.setName(null);

        assertThrows(IllegalArgumentException.class,
                () -> studentService.registerStudent(student));

        verify(studentRepository, never()).save(any(Student.class));
    }
}