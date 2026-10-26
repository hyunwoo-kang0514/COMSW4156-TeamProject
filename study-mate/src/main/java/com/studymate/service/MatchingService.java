package com.studymate.service;

import com.studymate.domain.Student;
import com.studymate.domain.StudyGroup;
import com.studymate.domain.TimeSlot;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Scores and ranks study groups that are compatible with a student's preference. */
@Service
public class MatchingService {

  private final StudyGroupService studyGroupService;
  private static final int MAX_MATCH_RESULT = 3;

  public MatchingService(final StudyGroupService studyGroupService) {
    this.studyGroupService = studyGroupService;
  }

  /**
   * Find compatible study groups.
   *
   * @param student instance of the student
   * @return list of group that match (max = MAX_MATCH_RESULT)
   */
  public List<StudyGroup> findCompatibleStudyGroups(Student student) {
    // Get study groups that meet basic criteria: still recruiting, match school, match course,
    // match mode
    List<StudyGroup> candidates =
        studyGroupService.getCandidateGroups(
            student.getSchool(),
            student.getPreference().getCourse(),
            student.getPreference().getMeetingMode());

    // if there is no study groups that match basic criteria, just return
    if (candidates.isEmpty()) {
      return candidates;
    }
    return getMatchResult(getScores(student, candidates));
  }

  private Map<StudyGroup, Integer> getScores(Student student, List<StudyGroup> candidates) {

    List<TimeSlot> prefTimes = student.getPreference().getAvailableTimes();
    List<String> prefStyles = student.getPreference().getStudyStyles();
    int prefSize = student.getPreference().getPreferredGroupSize();
    Map<StudyGroup, Integer> scores = new HashMap<>();

    // Ranking top 3 groups
    for (StudyGroup group : candidates) {
      int score = 0;
      // case that say it is recruiting but already fulled
      if (group.getMemberIds().size() == group.getMaxMembers()) {
        continue;
      }

      // assume that group size must be greater or equal than two
      if (group.getMaxMembers() == prefSize) {
        score += 25;
      } else if (group.getMaxMembers() >= prefSize - 1 && group.getMaxMembers() <= prefSize + 1) {
        score += 20;
      }

      // Assume that user and group must select at least two study styles and there is three in
      // total, which are unique
      int matchStyle = 0;
      for (String studentStyle : prefStyles) {
        for (String groupStyle : group.getStudyStyles()) {
          if (studentStyle.equals(groupStyle)) {
            matchStyle++;
          }
        }
      }

      if (matchStyle == prefStyles.size()) {
        score += 25;
      } else if (matchStyle >= 1) {
        score += 20;
      }

      // Calculate total overlapping time in minutes
      long overlapMinutes = 0;

      for (TimeSlot prefTime : prefTimes) {
        for (TimeSlot groupTime : group.getSchedules()) {

          // Different day -> cannot overlap
          if (prefTime.getDay() != groupTime.getDay()) {
            continue;
          }
          // Later start time
          LocalTime overlapStart =
              prefTime.getStartTime().isAfter(groupTime.getStartTime())
                  ? prefTime.getStartTime()
                  : groupTime.getStartTime();
          // Earlier end time
          LocalTime overlapEnd =
              prefTime.getEndTime().isBefore(groupTime.getEndTime())
                  ? prefTime.getEndTime()
                  : groupTime.getEndTime();
          // If start < end, there is an overlap
          if (overlapStart.isBefore(overlapEnd)) {
            overlapMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
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
   * Get match result.
   *
   * @param scores scores of associated study group
   * @return list of match result
   */
  private List<StudyGroup> getMatchResult(Map<StudyGroup, Integer> scores) {
    List<Map.Entry<StudyGroup, Integer>> rankedGroups = new ArrayList<>(scores.entrySet());
    rankedGroups.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
    List<StudyGroup> matchResult = new ArrayList<>();
    for (int i = 0; i < rankedGroups.size() && i < MAX_MATCH_RESULT; i++) {
      matchResult.add(rankedGroups.get(i).getKey());
    }
    return matchResult;
  }
}
