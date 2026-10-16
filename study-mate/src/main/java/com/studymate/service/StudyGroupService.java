package com.studymate.service;

import com.studymate.domain.StudyGroup;
import com.studymate.repository.StudyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;

    @Autowired
    public StudyGroupService(StudyGroupRepository studyGroupRepository) {
        this.studyGroupRepository = studyGroupRepository;
    }

    /**
     * Get all groups that exist in the database
     * @return all groups that exist
     */
    public List<StudyGroup> getAllStudyGroups() {
        return studyGroupRepository.findAll();
    }

    /**
     * Get all candidate groups from the database
     */
    public List<StudyGroup> getCandidateGroups(String school, String course, String mode) {
        return studyGroupRepository.findCandidateGroups(school, course, mode);
    }
}