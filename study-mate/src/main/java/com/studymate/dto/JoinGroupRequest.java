package com.studymate.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for joining a study group.
 * AI-generated (Claude Code), introduced to fix a double-@RequestBody binding bug
 * on the /api/join endpoint. See README "AI Documentation" section for details.
 */
@Getter
@Setter
public class JoinGroupRequest {
    private Long studentId;
    private Long groupId;
}
