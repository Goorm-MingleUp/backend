package com.mingleup.backend.domain.review.controller;

import com.mingleup.backend.domain.review.dto.BulkCreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewResponse; // [추가]
import com.mingleup.backend.domain.review.service.ReviewService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema; // [추가]
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

import java.util.List; // [추가]

@Tag(name = "Review", description = "후기 API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 후기 작성 API (일괄/단건 공용)
     * [POST] /api/v1/reviews/bulk
     */
    @Operation(
            summary = "후기 작성 (일괄/단건)",
            description = """
            여러 개의 후기를 한 번의 요청으로 생성합니다. (트랜잭션 보장)
            성공 시 생성된 후기들의 기본 정보(ID, Type 등)를 반환합니다.
            
            - **HOST/PARTICIPANT**: `revieweeId` 필수
            - **AI_GROUP**: `aiGroupId` 필수
            - **PARTY**: `partyId`만 필요
            
            **[중요]** 모임에 '참석 완료(ATTENDED)' 상태인 경우에만 작성 가능합니다.
            """,
            tags = {"Review"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateReviewResponse.class), // [추가] 스키마 정의
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": [
                                    {
                                        "reviewId": 101,
                                        "reviewType": "PARTY",
                                        "partyId": 5
                                    },
                                    {
                                        "reviewId": 102,
                                        "reviewType": "HOST",
                                        "partyId": 5,
                                        "revieweeId": 6
                                    }
                                ]
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "입력 값 오류 (하나라도 실패하면 전체 롤백)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (참석하지 않음 등)")
    })
    @PostMapping("/bulk")
    public ResponseEntity<ApiResult<List<CreateReviewResponse>>> createBulkReviews( // [수정] 반환 타입 변경 Void -> List<...>
                                                                                    Authentication authentication,
                                                                                    @Valid @RequestBody BulkCreateReviewRequest request
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        List<CreateReviewResponse> responses = reviewService.createBulkReviews(currentUserId, request); // [수정]
        return ResponseEntity.ok(ApiResult.onSuccess(responses)); // [수정]
    }
}