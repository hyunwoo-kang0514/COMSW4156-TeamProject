package com.studymate.repository;

import com.studymate.domain.StudyGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** JPA repository for {@link StudyGroup} records and matching candidate queries. */
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

  @Query(
      """
    SELECT g
    FROM StudyGroup g
    WHERE g.school = :school
      AND g.course = :course
      AND g.meetingMode = :meetingMode
      AND g.isRecruiting = true
      """)
  List<StudyGroup> findCandidateGroups(
      @Param("school") String school,
      @Param("course") String course,
      @Param("meetingMode") String meetingMode);
}
