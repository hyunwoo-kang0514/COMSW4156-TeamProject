package com.studymate.service;

import com.studymate.domain.Preference;
import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchingService {

    private final StudyGroupService studyGroupService;

    public MatchingService(final StudyGroupService studyGroupService) {
        this.studyGroupService = studyGroupService;
    }

    public ArrayList<StudyGroup> findCompatibleStudyGroups(Student student) {
        Preference preference = student.getPreference();
        // get all groups that are available
        List<StudyGroup> existingGroups = studyGroupService.getStudyGroups();
        int groupNum = existingGroups.size();

        for (StudyGroup group : existingGroups) {
            if (group.getMemberIds().size() == group.getMaxMembers()) {
                continue;
            }

            if (group.getCourse().equals(preference.getCourse())) {
                continue;
            }

        }

        return new ArrayList<>();
    }
}