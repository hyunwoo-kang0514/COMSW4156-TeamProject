package com.studymate.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.studymate.domain.Preference;


public interface PreferenceRepository extends JpaRepository<Preference, Long>{
}