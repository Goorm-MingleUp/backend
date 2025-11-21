package com.mingleup.backend.domain.application.controller;

import com.mingleup.backend.domain.application.dto.MyApplicationResponse;
import com.mingleup.backend.domain.application.service.PartyApplicationService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Application", description = "모임 신청 내역 API")
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PartyApplicationController {

    private final PartyApplicationService partyApplicationService;

    /**
     * 내 파티 신청 목록 조회 API
     */
    @Operation(
            summary = "내 신청 목록 조회",
            description = """
            내가 신청한 모든 모임의 목록과 상태(대기, 확정 등)를 최신순으로 조회합니다.
            """,
            tags = {"Application"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyApplicationResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "content": [
                                        {
                                            "applicationId": 101,
                                            "applicationStatus": "PENDING",
                                            "appliedAt": "2025-11-19T15:30:00",
                                            "partyId": 5,
                                            "partyTitle": "주말 풋살 모임",
                                            "partyImageUrl": "https://img.url/soccer.jpg",
                                            "partyDatetime": "2025-12-05T10:00:00",
                                            "partyLocationName": "잠실 풋살장"
                                        },
                                        {
                                            "applicationId": 102,
                                            "applicationStatus": "APPROVED",
                                            "appliedAt": "2025-11-18T09:00:00",
                                            "partyId": 4,
                                            "partyTitle": "전략 보드게임",
                                            "partyImageUrl": "https://img.url/board.jpg",
                                            "partyDatetime": "2025-12-01T18:00:00",
                                            "partyLocationName": "강남 보드카페"
                                        }
                                    ],
                                    "pageable": {
                                        "pageNumber": 0,
                                        "pageSize": 10,
                                        "sort": { "sorted": true, "unsorted": false, "empty": false },
                                        "offset": 0,
                                        "paged": true,
                                        "unpaged": false
                                    },
                                    "totalPages": 1,
                                    "totalElements": 2,
                                    "last": true,
                                    "size": 10,
                                    "number": 0,
                                    "sort": { "sorted": true, "unsorted": false, "empty": false },
                                    "numberOfElements": 2,
                                    "first": true,
                                    "empty": false
                                }
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResult<Page<MyApplicationResponse>>> getMyApplications(
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 (예: appliedAt,desc)") @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Long currentUserId = Long.parseLong(authentication.getName());
        Page<MyApplicationResponse> myApplications = partyApplicationService.getMyApplications(currentUserId, pageable);
        return ResponseEntity.ok(ApiResult.onSuccess(myApplications));
    }
}