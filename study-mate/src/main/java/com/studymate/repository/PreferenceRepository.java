package com.studymate.repository;

import com.studymate.domain.Preference;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for student study {@link Preference} records. */
public interface PreferenceRepository extends JpaRepository<Preference, Long> {}
