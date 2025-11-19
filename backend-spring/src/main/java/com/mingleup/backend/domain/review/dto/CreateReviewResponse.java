package com.mingleup.backend.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.review.domain.ReviewType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드 제외
public class CreateReviewResponse {

    private Long reviewId;
    private ReviewType reviewType;
    private Long partyId;
    private Long revieweeId; // 대상 유저 ID (HOST, PARTICIPANT인 경우)
    private Long aiGroupId;  // 대상 AI 그룹 ID (AI_GROUP인 경우)

    /**
     * Review 엔티티를 응답 DTO로 변환
     */
    public static CreateReviewResponse from(Review review) {
        return CreateReviewResponse.builder()
                .reviewId(review.getId())
                .reviewType(review.getReviewType())
                .partyId(review.getParty().getId())
                .revieweeId(review.getReviewee() != null ? review.getReviewee().getId() : null)
                .aiGroupId(review.getAiGroup() != null ? review.getAiGroup().getId() : null)
                .build();
    }
}