package com.studymate.service;

import org.springframework.stereotype.Service;
import com.studymate.domain.Preference;
import com.studymate.repository.PreferenceRepository;
import com.studymate.repository.StudentRepository;
import lombok.AllArgsConstructor;
import com.studymate.domain.Student;

@AllArgsConstructor
@Service
public class PreferenceService {
    private final PreferenceRepository preferenceRepository;
    private final StudentRepository studentRepository;
    /**
     * Saves a student's study preferences, replacing any existing ones for that student.
     *
     * @param preference the preferences to save
     * @return the saved preference
     */
    public Preference addStudentStudyPreferences(Preference preference){
        Preference savedPreference = preferenceRepository.save(preference);
        return savedPreference;
    }


    /**
     * Updates a student's study preferences. The studentId is not changed.
     *
     * @param id the studentId to update
     * @param preference the new values
     * @return the updated preference
     * @throws RuntimeException if no preference exists for the id
     */
    public Preference updateStudentStudyPreferences(Long id, Preference preference){
        Preference updatedPreference = preferenceRepository.findById(id)
            .orElseThrow(() -> 
            new RuntimeException("Preference not found for studentId " + id));
        Student student = studentRepository.findById(id)
            .orElseThrow(() ->
            new RuntimeException("Student not found for studentId " + id));
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
     * @throws RuntimeException if no preference exists for the id
     */
    public Preference getStudentStudyPreferences(Long id){
        Preference gottenPreference = preferenceRepository.findById(id)
            .orElseThrow(() -> 
            new RuntimeException("Preference not found for studentId " + id));
        return gottenPreference;
    }
}