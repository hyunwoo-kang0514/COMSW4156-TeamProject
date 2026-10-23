package com.studymate.service;

import org.springframework.stereotype.Service;
import com.studymate.domain.Preference;
import com.studymate.repository.PreferenceRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class PreferenceService {
    private final PreferenceRepository preferenceRepository;

    public Preference addStudentStudyPreferences(Preference preference){
        Preference savedPreference = preferenceRepository.save(preference);
        return savedPreference;
    }


    public Preference updateStudentStudyPreferences(Long id, Preference preference){
        Preference updatedPreference = preferenceRepository.findById(id)
            .orElseThrow(() -> 
            new RuntimeException("Preference not found for studentId " + id));
        updatedPreference.setSchool(preference.getSchool());
        updatedPreference.setYear(preference.getYear());
        updatedPreference.setCourse(preference.getCourse());
        updatedPreference.setSchedule(preference.getSchedule());
        updatedPreference.setPreferredGroupSize(preference.getPreferredGroupSize());
        Preference savedPreference = preferenceRepository.save(updatedPreference);
        return savedPreference;
    }

    public Preference getStudentStudyPreferences(Long id){
        Preference gottenPreference = preferenceRepository.findById(id)
            .orElseThrow(() -> 
            new RuntimeException("Preference not found for studentId " + id));
        return gottenPreference;
    }
}