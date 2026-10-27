package com.studymate.service;

import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.repository.PreferenceRepository;
import com.studymate.repository.StudentRepository;
import java.util.NoSuchElementException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, updates, and retrieves student study preferences. */
@AllArgsConstructor
@Service
public class PreferenceService {
  private final PreferenceRepository preferenceRepository;
  private final StudentRepository studentRepository;

  /**
   * Saves a student's study preferences, replacing any existing ones for that student.
   *
   * <p>The preference shares its primary key with the student, so the student named by the
   * preference's id is looked up and attached before saving. Hibernate derives the key from that
   * student, which is why the values are copied onto a fresh (or the existing) entity instead of
   * saving the request object with its id preset.
   *
   * @param preference the preferences to save; its id must be an existing student's id
   * @return the saved preference
   * @throws IllegalArgumentException if the preference has no id
   * @throws NoSuchElementException if no student exists for the id
   */
  @Transactional
  public Preference addStudentStudyPreferences(Preference preference) {
    if (preference.getId() == null) {
      throw new IllegalArgumentException("id is required and must be an existing student id.");
    }
    Long id = preference.getId();
    Student student =
        studentRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Student not found for studentId " + id));
    Preference target = preferenceRepository.findById(id).orElseGet(Preference::new);
    target.setStudent(student);
    target.setCourse(preference.getCourse());
    target.setAvailableTimes(preference.getAvailableTimes());
    target.setStudyStyles(preference.getStudyStyles());
    target.setPreferredGroupSize(preference.getPreferredGroupSize());
    target.setMeetingMode(preference.getMeetingMode());
    return preferenceRepository.save(target);
  }

  /**
   * Updates a student's study preferences. The studentId is not changed.
   *
   * @param id the studentId to update
   * @param preference the new values
   * @return the updated preference
   * @throws NoSuchElementException if no preference or student exists for the id
   */
  // Partially AI-fixed (Claude)
  public Preference updateStudentStudyPreferences(Long id, Preference preference) {
    Preference updatedPreference =
        preferenceRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Preference not found for studentId " + id));
    Student student =
        studentRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Student not found for studentId " + id));
    updatedPreference.setAvailableTimes(preference.getAvailableTimes());
    updatedPreference.setCourse(preference.getCourse());
    updatedPreference.setStudent(student);
    updatedPreference.setStudyStyles(preference.getStudyStyles());
    updatedPreference.setPreferredGroupSize(preference.getPreferredGroupSize());
    updatedPreference.setMeetingMode(preference.getMeetingMode());
    Preference savedPreference = preferenceRepository.save(updatedPreference);
    return savedPreference;
  }

  /**
   * Retrieves a student's study preferences.
   *
   * @param id the studentId to look up
   * @return the stored preference
   * @throws NoSuchElementException if no preference exists for the id
   */
  public Preference getStudentStudyPreferences(Long id) {
    Preference gottenPreference =
        preferenceRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Preference not found for studentId " + id));
    return gottenPreference;
  }
}
