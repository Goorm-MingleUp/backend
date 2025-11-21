package com.mingleup.backend.domain.review.controller;

import com.mingleup.backend.domain.review.dto.BulkCreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewResponse;
import com.mingleup.backend.domain.review.service.ReviewService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Review", description = "후기 작성 API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 후기 작성 API (일괄/단건 공용)
     */
    @Operation(
            summary = "후기 작성 (일괄/단건)",
            description = """
            파티 종료 후 여러 종류의 후기(파티, 호스트, 참가자, AI그룹)를 한 번에 작성합니다.
            하나라도 실패하면 모두 롤백됩니다 (All or Nothing).
            
            **조건:** 해당 파티에 `ATTENDED` (참석 완료) 상태여야 합니다.
            """,
            tags = {"Review"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateReviewResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": [
                                    {
                                        "reviewId": 101,
                                        "reviewType": "PARTY",
                                        "partyId": 5,
                                        "revieweeId": null,
                                        "aiGroupId": null
                                    },
                                    {
                                        "reviewId": 102,
                                        "reviewType": "HOST",
                                        "partyId": 5,
                                        "revieweeId": 6,
                                        "aiGroupId": null
                                    }
                                ]
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "중복 작성 등 입력 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": false,
                                "code": "COMMON4001",
                                "message": "유효하지 않은 입력 값입니다.",
                                "result": "해당 모임에서 이 유저에 대한 후기를 이미 작성했습니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음 (미참석)")
    })
    @PostMapping("/bulk")
    public ResponseEntity<ApiResult<List<CreateReviewResponse>>> createBulkReviews(
            Authentication authentication,
            @Valid @RequestBody BulkCreateReviewRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        List<CreateReviewResponse> responses = reviewService.createBulkReviews(currentUserId, request);
        return ResponseEntity.ok(ApiResult.onSuccess(responses));
    }
}