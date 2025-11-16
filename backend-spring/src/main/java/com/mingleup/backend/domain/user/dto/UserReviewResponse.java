package com.mingleup.backend.domain.user.dto;

import com.mingleup.backend.domain.review.domain.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 5: GET /api/v1/users/{userId}/reviews (유저 후기 목록 조회) 응답 DTO
 */
@Getter
@Builder
public class UserReviewResponse {

    private Long reviewId;
    private Long reviewerId;
    private String reviewerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    /**
     * Review 엔티티를 UserReviewResponse DTO로 변환합니다.
     * @param review
     * @return
     */
    public static UserReviewResponse from(Review review) {
        // (주의) N+1 문제가 발생할 수 있습니다. 추후 Fetch Join 등으로 최적화가 필요합니다.
        String reviewerName = (review.getReviewer() != null) ? review.getReviewer().getName() : "익명";
        Long reviewerId = (review.getReviewer() != null) ? review.getReviewer().getId() : null;

        return UserReviewResponse.builder()
                .reviewId(review.getId())
                .reviewerId(reviewerId)
                .reviewerName(reviewerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}