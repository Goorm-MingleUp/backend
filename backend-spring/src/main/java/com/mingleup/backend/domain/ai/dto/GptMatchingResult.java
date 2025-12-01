package com.mingleup.backend.domain.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GptMatchingResult {
    private List<GroupResult> groups;

    @Getter
    @NoArgsConstructor
    public static class GroupResult {
        private String groupName;
        private String reason;
        private List<Long> userIds;
    }
}