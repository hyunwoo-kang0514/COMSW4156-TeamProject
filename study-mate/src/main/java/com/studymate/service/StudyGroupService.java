package com.studymate.service;

import com.studymate.domain.StudyGroup;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudyGroupService {


    /**
     * Get all groups that exist
     * @return list of groups that exist
     */
    public List<StudyGroup> getStudyGroups() {
        ArrayList<StudyGroup> groups = new ArrayList<>();
        return groups;
    }
}