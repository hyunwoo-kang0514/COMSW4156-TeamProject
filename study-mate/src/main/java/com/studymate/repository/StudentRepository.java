package com.studymate.repository;
import com.studymate.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByApiKey(String apiKey);
}