package com.mingleup.backend.domain.host.controller;

import com.mingleup.backend.domain.host.dto.HostDashboardResponse;
import com.mingleup.backend.domain.host.dto.HostPartyResponse;
import com.mingleup.backend.domain.host.dto.UpdatePartyStatusRequest; // [추가]
import com.mingleup.backend.domain.host.service.HostService;
import com.mingleup.backend.domain.party.domain.PartyStatus;
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
import jakarta.validation.Valid; // [추가]
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; // [수정] RequestMapping 등 포함

@Tag(name = "Host", description = "호스트 관리 API")
@RestController
@RequestMapping("/api/v1/host")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class HostController {

    private final HostService hostService;

    /**
     * 호스트 대시보드 요약 조회 API
     * [GET] /api/v1/host/dashboard
     */
    @Operation(
            summary = "호스트 대시보드 요약 조회",
            description = """
            호스트 대시보드 상단에 표시될 프로필 정보와 통계 수치를 조회합니다.
            
            - **hostedCount**: 내가 만든 총 파티 수
            - **pendingApprovalCount**: 내 파티 승인을 기다리는 대기자 수 (전체 파티 합산)
            - **completedCount**: 종료(완료)된 내 파티 수
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HostDashboardResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "hostName": "박호스트",
                                    "hostProfileImageUrl": "https://example.com/img/host.jpg",
                                    "hostIntro": "안녕하세요! 베이킹 호스트입니다.",
                                    "avgRating": 4.5,
                                    "hostNickname": "친절한호스트",
                                    "hostedCount": 5,
                                    "pendingApprovalCount": 3,
                                    "completedCount": 2
                                }
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<HostDashboardResponse>> getDashboardSummary(
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        HostDashboardResponse dashboard = hostService.getDashboardSummary(userId);
        return ResponseEntity.ok(ApiResult.onSuccess(dashboard));
    }

    /**
     * 호스트가 생성한 파티 목록 조회 API
     * [GET] /api/v1/host/parties
     */
    @Operation(
            summary = "호스트 생성 파티 목록 조회",
            description = """
            호스트가 자신이 생성한 파티 목록을 조회합니다. (대시보드 하단 리스트)
            
            **필터링:** `status` 파라미터로 파티 상태(`RECRUITING`, `CLOSED`, `COMPLETED` 등)를 필터링할 수 있습니다.
            **페이징:** `page`, `size`, `sort` 파라미터를 지원합니다.
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HostPartyResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "content": [
                                        {
                                            "partyId": 10,
                                            "title": "이번 주말 보드게임 하실 분",
                                            "partyImageUrl": "https://example.com/img/boardgame.jpg",
                                            "partyDatetime": "2025-12-01T14:00:00",
                                            "locationName": "강남역 보드게임카페",
                                            "status": "RECRUITING",
                                            "minParticipants": 2,
                                            "maxParticipants": 4,
                                            "entryFee": 5000,
                                            "createdAt": "2025-11-15T10:00:00"
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
                                    "totalElements": 1,
                                    "last": true,
                                    "size": 10,
                                    "number": 0,
                                    "sort": { "sorted": true, "unsorted": false, "empty": false },
                                    "numberOfElements": 1,
                                    "first": true,
                                    "empty": false
                                }
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/parties")
    public ResponseEntity<ApiResult<Page<HostPartyResponse>>> getHostParties(
            Authentication authentication,
            @Parameter(description = "파티 상태 필터 (예: RECRUITING, CLOSED, COMPLETED)", example = "RECRUITING")
            @RequestParam(required = false) PartyStatus status,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc")
            @RequestParam(required = false) String sort
    ) {
        // 기본 정렬: 생성일 내림차순 (최신순)
        Pageable pageable;
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");

        if (sort != null && !sort.isEmpty()) {
            try {
                String[] sortParams = sort.split(",");
                Sort.Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")) ?
                        Sort.Direction.DESC : Sort.Direction.ASC;
                pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
            } catch (Exception e) {
                pageable = PageRequest.of(page, size, defaultSort);
            }
        } else {
            pageable = PageRequest.of(page, size, defaultSort);
        }

        Long userId = Long.parseLong(authentication.getName());
        Page<HostPartyResponse> parties = hostService.getHostParties(userId, status, pageable);

        return ResponseEntity.ok(ApiResult.onSuccess(parties));
    }

    /**
     * 파티 모집 상태 변경 API (호스트 전용)
     * [PATCH] /api/v1/host/parties/{partyId}/status
     */
    @Operation(
            summary = "파티 모집 상태 변경",
            description = """
            호스트가 자신이 만든 파티의 모집 상태를 수동으로 변경합니다.
            예: 모집중(RECRUITING) -> 모집마감(CLOSED)
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 상태값"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 파티가 아님)"),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음")
    })
    @PatchMapping("/parties/{partyId}/status")
    public ResponseEntity<ApiResult<Void>> updatePartyStatus(
            Authentication authentication,
            @Parameter(description = "상태를 변경할 파티의 ID", required = true)
            @PathVariable Long partyId,
            @Valid @RequestBody UpdatePartyStatusRequest request
    ) {
        Long userId = Long.parseLong(authentication.getName());
        hostService.updatePartyStatus(userId, partyId, request.getStatus());
        return ResponseEntity.ok(ApiResult.onSuccess());
    }
}