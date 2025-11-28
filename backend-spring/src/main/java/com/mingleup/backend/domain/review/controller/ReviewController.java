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
import com.mingleup.backend.domain.review.dto.BulkCreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewResponse;
import com.mingleup.backend.domain.review.service.ReviewService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@Tag(name = "Review", description = "후기 작성 API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "파티 후기 목록 조회",
            description = "특정 파티의 후기 목록을 조회합니다. (로그인 불필요)",
            security = {}
    )
    @GetMapping("/{partyId}/reviews")
    public ApiResult<PartyReviewListResponse> getPartyReviews(
            @PathVariable Long partyId
    ) {
        PartyReviewListResponse response = reviewService.getReviewsByParty(partyId);
        return ApiResult.onSuccess(response);
    }

    /**
     * 후기 작성 API (일괄/단건 공용)
     */
    @Operation(
            summary = "후기 작성 (일괄/단건) - 등록/수정",
            description = """
            여러 개의 후기를 한 번의 요청으로 생성하거나 수정합니다. (Upsert Logic)
            
            - **신규 작성**: 해당 대상에 대한 후기가 없으면 새로 등록합니다.
            - **수정**: 이미 작성한 후기가 있다면 내용을 **수정(Update)**합니다.
            - 하나라도 처리에 실패하면 전체 롤백됩니다 (Transaction).
            
            **조건:** 해당 파티에 `ATTENDED` (참석 완료) 상태여야 합니다.
            """,
            tags = {"Review"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "작성 또는 수정 성공",
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
                    description = "입력 값 오류 (ID 누락, 평점 범위 오류 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": false,
                                "code": "COMMON4001",
                                "message": "유효하지 않은 입력 값입니다.",
                                "result": "HOST 후기에는 revieweeId가 필수입니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음 (미참석 등)")
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
