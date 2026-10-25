package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.repository.StudyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        // AI-fixed (Claude Code): this previously never persisted the new membership
        // (missing repository.save() call) — see README "AI Documentation" section.
        return studyGroupRepository.save(studyGroup);
    }

    /**
     * Remove a student from a group and persist the change.
     * Every occurrence of the student's id is removed from memberIds (guards against
     * duplicate rows left by older data), and numMembers is decremented by the number
     * of ids removed, never going below zero. The group itself is kept even if it
     * becomes empty, and its isRecruiting flag is left unchanged.
     * The caller is expected to check isMember() first; if the student is not a
     * member this method changes nothing and does not touch the database.
     * AI-generated (Claude Code), reviewed by Ziyue Fang. See README "AI Documentation".
     *
     * @param student instance of the student leaving
     * @param studyGroup instance of the study group
     * @return the saved group reflecting the updated membership
     */
    @Transactional
    public StudyGroup leaveGroup(Student student, StudyGroup studyGroup) {
        List<String> memberIds = studyGroup.getMemberIds();
        int sizeBefore = memberIds.size();
        memberIds.removeIf(member -> member.equals(student.getStudentId()));
        int removed = sizeBefore - memberIds.size();
        if (removed == 0) {
            return studyGroup;
        }
        studyGroup.setMemberIds(memberIds);
        studyGroup.setNumMembers(Math.max(0, studyGroup.getNumMembers() - removed));
        return studyGroupRepository.save(studyGroup);
    }
}