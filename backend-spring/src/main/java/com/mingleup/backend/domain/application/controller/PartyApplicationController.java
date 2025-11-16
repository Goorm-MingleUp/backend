package com.mingleup.backend.domain.application.controller; // [추가] 패키지 선언

import com.mingleup.backend.domain.application.dto.MyApplicationResponse; // [추가]
import com.mingleup.backend.domain.application.service.PartyApplicationService; // [추가]
import com.mingleup.backend.global.common.ApiResult; // [추가]
import io.swagger.v3.oas.annotations.Operation; // [추가]
import io.swagger.v3.oas.annotations.Parameter; // [수정] import 추가
import io.swagger.v3.oas.annotations.media.Content; // [추가]
import io.swagger.v3.oas.annotations.media.ExampleObject; // [추가]
import io.swagger.v3.oas.annotations.media.Schema; // [추가]
import io.swagger.v3.oas.annotations.responses.ApiResponse; // [추가]
import io.swagger.v3.oas.annotations.responses.ApiResponses; // [추가]
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // [추가]
import io.swagger.v3.oas.annotations.tags.Tag; // [추가]
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.data.web.PageableDefault; // [추가]
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // [추가]
import org.springframework.web.bind.annotation.GetMapping; // [추가]
import org.springframework.web.bind.annotation.RequestMapping; // [추가]
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController; // [추가]

import java.util.List;

@Tag(name = "Application", description = "모임 신청 API")
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PartyApplicationController {

    private final PartyApplicationService partyApplicationService;

    /**
     * 내 파티 신청 목록 조회 API
     * [GET] /api/v1/applications/me
     */
    @Operation(
            summary = "내 파티 신청 목록 조회",
            description = "현재 로그인한 사용자가 신청한 모든 모임 목록과 신청 상태를 조회합니다.",
            tags = {"Application"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "신청 목록 조회 성공",
                    // [추가] 상세한 응답 예시
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
                                            "applicationId": 11,
                                            "applicationStatus": "PENDING",
                                            "appliedAt": "2025-11-17T03:22:22",
                                            "partyId": 4,
                                            "partyTitle": "호스트1(보드킹)의 전략 게임 모임",
                                            "partyImageUrl": "https://example.com/img/party4.jpg",
                                            "partyDatetime": "2025-12-01T18:00:00",
                                            "partyLocationName": "강남 보드게임카페"
                                          },
                                          {
                                            "applicationId": 12,
                                            "applicationStatus": "APPROVED",
                                            "appliedAt": "2025-11-17T03:22:22",
                                            "partyId": 5,
                                            "partyTitle": "호스트2(스포츠짱)의 주말 풋살 모임",
                                            "partyImageUrl": "https://example.com/img/party5.jpg",
                                            "partyDatetime": "2025-12-05T10:00:00",
                                            "partyLocationName": "잠실 풋살장"
                                          }
                                        ],
                                        "pageable": {
                                          "pageNumber": 0,
                                          "pageSize": 10,
                                          "sort": {
                                            "sorted": false,
                                            "unsorted": true,
                                            "empty": true
                                          },
                                          "offset": 0,
                                          "paged": true,
                                          "unpaged": false
                                        },
                                        "last": true,
                                        "totalPages": 1,
                                        "totalElements": 2,
                                        "first": true,
                                        "numberOfElements": 2,
                                        "size": 10,
                                        "number": 0,
                                        "sort": {
                                          "sorted": false,
                                          "unsorted": true,
                                          "empty": true
                                        },
                                        "empty": false
                                      }
                                    }
                            """)
                    )
            ),
            // --- [수정] 401, 404 실패 응답 예시 추가 ---
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                 "success": false,
                                 "code": "AUTH4001",
                                 "message": "인증에 실패했습니다.",
                                 "result": "유효한 인증 정보가 없습니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                 "success": false,
                                 "code": "USER4004",
                                 "message": "데이터를 찾을 수 없습니다.",
                                 "result": "사용자 정보를 찾을 수 없습니다."
                            }
                            """)
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResult<Page<MyApplicationResponse>>> getMyApplications(
         Authentication authentication,
         @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
         @RequestParam(defaultValue = "0") int page,

         @Parameter(description = "페이지 크기", example = "10")
         @RequestParam(defaultValue = "10") int size,

         @Parameter(description = "정렬 기준 (예: appliedAt,desc)", example = "appliedAt,desc")
         @RequestParam(required = false) String sort
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Long currentUserId = Long.parseLong(authentication.getName());

        Page<MyApplicationResponse> myApplications =
                partyApplicationService.getMyApplications(currentUserId, pageable);

        return ResponseEntity.ok(ApiResult.onSuccess(myApplications));
    }

    // (TODO: 모임 신청(POST), 신청 취소(DELETE), 신청 승인/거절(PATCH) 등 API 추가)
}