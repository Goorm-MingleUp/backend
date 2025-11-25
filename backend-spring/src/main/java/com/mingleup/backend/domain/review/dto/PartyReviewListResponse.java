package com.mingleup.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "파티 후기 목록 응답")
public record PartyReviewListResponse(
        Long partyId,
        List<PartyReviewResponse> reviews
) { }