package com.mingleup.backend.domain.review.controller;

import com.mingleup.backend.domain.review.dto.PartyReviewListResponse;
import com.mingleup.backend.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review", description = "후기 작성 API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "파티 후기 목록 조회")
    @GetMapping("/{partyId}/reviews")
    public ResponseEntity<PartyReviewListResponse> getPartyReviews(
            @PathVariable Long partyId
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByParty(partyId));
    }

}
