package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.repository.StudyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    /**
     * Find study group by id
     *
     * @param groupId id of the student
     * @return corresponding Student object, return null if DNE
     */
    public StudyGroup findStudyGroupById(Long groupId) {
        return studyGroupRepository.findById(groupId).orElse(null);
    }

    /**
     * Check whether a student is in the group
     * @param student instance of student
     * @param studyGroup instance of study group
     * @return True if already member, false otherwise
     */
    public boolean isMember(Student student, StudyGroup studyGroup) {
        String studentId = student.getStudentId();
        List<String> members = studyGroup.getMemberIds();

        for (String member : members) {
            if (member.equals(studentId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check availability of the group
     * @param studyGroup instance of the study group
     * @return True if can join, false otherwise
     */
    public boolean canJoin(StudyGroup studyGroup) {
        return studyGroup.getNumMembers() < studyGroup.getMaxMembers() && studyGroup.isRecruiting();
    }

    /**
     * Join a student to a group
     * @param student instance of the student
     * @param studyGroup instance of study group
     */
    public StudyGroup joinGroup(Student student, StudyGroup studyGroup) {
        List<String> newMemberIds = studyGroup.getMemberIds();
        newMemberIds.add(student.getStudentId());
        studyGroup.setMemberIds(newMemberIds);
        studyGroup.setNumMembers(studyGroup.getNumMembers() + 1);
        // TO DO: need to update the DB here
        return studyGroup;
    }
}