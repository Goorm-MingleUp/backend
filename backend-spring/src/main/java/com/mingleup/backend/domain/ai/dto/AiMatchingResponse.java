package com.mingleup.backend.domain.ai.dto;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class AiMatchingResponse {

    private Long groupId;
    private String groupName;
    private String matchingReason;
    private List<GroupMemberDto> members;

    public static AiMatchingResponse from(AiGroup aiGroup) {
        return AiMatchingResponse.builder()
                .groupId(aiGroup.getId())
                .groupName(aiGroup.getGroupName())
                .matchingReason(aiGroup.getMatchingReason())
                .members(aiGroup.getMembers().stream()
                        .map(member -> GroupMemberDto.from(member.getUser()))
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class GroupMemberDto {
        private Long userId;
        private String name;
        private String profileImageUrl;
        private String mbti;

        public static GroupMemberDto from(User user) {
            return GroupMemberDto.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .mbti(user.getMbti())
                    .build();
        }
    }
}