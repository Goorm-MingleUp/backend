package com.mingleup.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "파티 후기 DTO")
public record PartyReviewResponse(
        Long reviewId,
        Long reviewerId,
        String reviewerNickname,
        String reviewerProfileUrl,
        int rating,
        String content,
        LocalDateTime createdAt
) { }