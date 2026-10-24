package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchingService {

    private final StudyGroupService studyGroupService;
    private static final int MAX_MATCH_RESULT = 3;

    public MatchingService(final StudyGroupService studyGroupService) {
        this.studyGroupService = studyGroupService;
    }

    /**
     * Find compatible study groups
     * @param student instance of the student
     * @return list of group that match (max = MAX_MATCH_RESULT)
     */
    public List<StudyGroup> findCompatibleStudyGroups(Student student) {
        // Get study groups that meet basic criteria: still recruiting, match school, match course, match mode
        List<StudyGroup> candidates = studyGroupService.getCandidateGroups(
                  student.getSchool()
                 ,student.getPreference().getCourse()
                 ,student.getPreference().getMeetingMode());

        // if there is no study groups that match basic criteria, just return
        if (candidates.isEmpty()) {
            return candidates;
        }
        return getMatchResult(getScores(student, candidates));
    }

    private Map<StudyGroup, Integer> getScores(Student student, List<StudyGroup> candidates) {

        List<TimeSlot> pTimes = student.getPreference().getAvailableTimes();
        List<String> pStyles = student.getPreference().getStudyStyles();
        int pSize = student.getPreference().getPreferredGroupSize();
        Map<StudyGroup, Integer> scores = new HashMap<>();

        // Ranking top 3 groups
        for (StudyGroup group : candidates) {
            int score = 0;
            // case that say it is recruiting but already fulled
            if (group.getMemberIds().size() == group.getMaxMembers()) {
                continue;
            }

            // assume that group size must be greater or equal than two
            if (group.getMaxMembers() == pSize) {
                score += 25;
            }
            else if (group.getMaxMembers() >= pSize - 1 && group.getMaxMembers() <= pSize + 1) {
                score += 20;
            }

            // Assume that user and group must select at least two study styles and there is three in total, which are unique
            int matchStyle = 0;
            for (String sStyle : pStyles) {
                for (String gStyle : group.getStudyStyles()) {
                    if (sStyle.equals(gStyle)) {
                        matchStyle++;
                    }
                }
            }

            if (matchStyle == pStyles.size()) {
                score += 25;
            } else if (matchStyle >= 1) {
                score += 20;
            }

            // Calculate total overlapping time in minutes
            long overlapMinutes = 0;

            for (TimeSlot pTime : pTimes) {
                for (TimeSlot gTime : group.getSchedules()) {

                    // Different day -> cannot overlap
                    if (pTime.getDay() != gTime.getDay()) {
                        continue;
                    }
                    // Later start time
                    LocalTime overlapStart =
                            pTime.getStartTime().isAfter(gTime.getStartTime())
                                    ? pTime.getStartTime()
                                    : gTime.getStartTime();
                    // Earlier end time
                    LocalTime overlapEnd =
                            pTime.getEndTime().isBefore(gTime.getEndTime())
                                    ? pTime.getEndTime()
                                    : gTime.getEndTime();
                    // If start < end, there is an overlap
                    if (overlapStart.isBefore(overlapEnd)) {
                        overlapMinutes += Duration
                                .between(overlapStart, overlapEnd)
                                .toMinutes();
                    }
                }
            }
            if (overlapMinutes >= 120) {
                score += 30;
            } else if (overlapMinutes >= 60) {
                score += 25;
            } else if (overlapMinutes > 0) {
                score += 15;
            }
            // the total is 80
            scores.put(group, score);
        }
        return scores;
    }

    /**
     * Get match result
     * @param scores scores of associated study group
     * @return list of match result
     */
    private List<StudyGroup> getMatchResult(Map<StudyGroup, Integer> scores) {
        List<Map.Entry<StudyGroup, Integer>> rankedGroups =
                new ArrayList<>(scores.entrySet());
        rankedGroups.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<StudyGroup> matchResult = new ArrayList<>();
        for (int i = 0; i < rankedGroups.size() && i < MAX_MATCH_RESULT; i++) {
            matchResult.add(rankedGroups.get(i).getKey());
        }
        return matchResult;
    }



}