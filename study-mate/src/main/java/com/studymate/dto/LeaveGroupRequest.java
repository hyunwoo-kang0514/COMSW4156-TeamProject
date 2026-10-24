package com.studymate.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for leaving a study group (POST /api/leave).
 * Mirrors {@link JoinGroupRequest} so join and leave share the same contract.
 * AI-generated (Claude Code), reviewed by Ziyue Fang. See README "AI Documentation".
 */
@Getter
@Setter
public class LeaveGroupRequest {
    /** Database id of the student who is leaving (Student.id). */
    private Long studentId;
    /** Id of the group being left (StudyGroup.groupId). */
    private Long groupId;
}
