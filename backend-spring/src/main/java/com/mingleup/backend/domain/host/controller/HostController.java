package com.mingleup.backend.domain.host.controller;

import com.mingleup.backend.domain.ai.dto.AiMatchingResponse;
import com.mingleup.backend.domain.ai.service.AiMatchingService;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.host.dto.*;
import com.mingleup.backend.domain.host.service.HostService;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import com.mingleup.backend.global.common.ApiResult;
import com.mingleup.backend.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mingleup.backend.global.common.PageResponse; // [추가]

import java.util.List;

@Tag(name = "Host", description = "호스트 관리(대시보드) API")
@RestController
@RequestMapping("/api/v1/host")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class HostController {

    private final HostService hostService;
    private final AiMatchingService aiMatchingService;
    /**
     * 호스트 대시보드 요약 조회 API
     */
    @Operation(
            summary = "호스트 대시보드 요약 조회",
            description = """
            호스트 대시보드 최상단에 표시될 **프로필 정보**와 **핵심 통계 수치**를 조회합니다.
            
            - **hostedCount**: 내가 지금까지 만든 파티의 총 개수
            - **pendingApprovalCount**: 내 모든 파티에서 승인을 기다리고 있는 대기자의 총 합계
            - **completedCount**: 종료(COMPLETED)된 내 파티 수
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
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<HostDashboardResponse>> getDashboardSummary(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(ApiResult.onSuccess(hostService.getDashboardSummary(userId)));
    }

    /**
     * [수정] 호스트 생성 파티 전체 목록 조회 API (상태 필터 없음)
     */
    @Operation(
            summary = "호스트 생성 파티 전체 목록 조회",
            description = """
            호스트가 자신이 생성한 **모든 파티**의 목록을 최신순으로 조회합니다.
            (페이징 정보 간소화 적용)
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "content": [
                                        {
                                            "partyId": 10,
                                            "title": "주말 보드게임",
                                            "status": "RECRUITING",
                                            "createdAt": "2025-11-20T10:00:00"
                                        }
                                    ],
                                    "pageNumber": 0,
                                    "pageSize": 10,
                                    "totalElements": 15
                                }
                            }
                            """)
                    )
            )
    })
    @GetMapping("/parties")
    public ResponseEntity<ApiResult<PageResponse<HostPartyResponse>>> getAllHostParties( // [수정] 반환 타입 변경
         Authentication authentication,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "10") int size,
         @RequestParam(required = false) String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Long userId = Long.parseLong(authentication.getName());

        // status = null -> 전체 조회
        Page<HostPartyResponse> parties = hostService.getHostParties(userId, null, pageable);

        // [수정] PageResponse로 감싸서 반환
        return ResponseEntity.ok(ApiResult.onSuccess(PageResponse.of(parties)));
    }

    /**
     * [신규] 호스트 생성 파티 상태별 목록 조회 API (분리됨)
     */
    @Operation(
            summary = "호스트 생성 파티 상태별 목록 조회",
            description = """
            호스트가 자신이 생성한 파티를 **상태별(RECRUITING, CLOSED, COMPLETED)**로 필터링하여 조회합니다.
            """,
            tags = {"Host"}
    )
    @GetMapping("/parties/status/{status}")
    public ResponseEntity<ApiResult<PageResponse<HostPartyResponse>>> getHostPartiesByStatus(
            Authentication authentication,
            @Parameter(description = "조회할 파티 상태", required = true, example = "RECRUITING")
            @PathVariable PartyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Long userId = Long.parseLong(authentication.getName());

        // 특정 상태 조회
        Page<HostPartyResponse> parties = hostService.getHostParties(userId, status, pageable);

        return ResponseEntity.ok(ApiResult.onSuccess(PageResponse.of(parties)));
    }

    /**
     * 파티 모집 상태 변경 API (호스트 전용)
     */
    @Operation(
            summary = "파티 모집 상태 변경",
            description = """
            호스트가 자신이 만든 파티의 모집 상태를 수동으로 변경합니다.
            예: 인원이 다 찼거나 사정상 조기 마감할 때 사용합니다.
            
            **가능한 상태값:** `RECRUITING`(모집중), `CLOSED`(마감), `COMPLETED`(종료), `CANCELED`(취소)
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상태 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 상태값 입력"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 파티가 아님)"),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음")
    })
    @PatchMapping("/parties/{partyId}/status")
    public ResponseEntity<ApiResult<Void>> updatePartyStatus(
            Authentication authentication,
            @PathVariable Long partyId,
            @Valid @RequestBody UpdatePartyStatusRequest request
    ) {
        Long userId = Long.parseLong(authentication.getName());
        hostService.updatePartyStatus(userId, partyId, request.getStatus());
        return ResponseEntity.ok(ApiResult.onSuccess());
    }

    /**
     * 파티 신청자 명단 조회 API (호스트 전용)
     */
    @Operation(
            summary = "파티 신청자 명단 조회",
            description = """
            특정 파티(`partyId`)에 신청한 사람들의 목록을 상태(`status`)별로 조회합니다.
            신청자의 프로필 정보뿐만 아니라, **호스트 질문에 대한 답변(`answerText`)**도 함께 제공됩니다.
            
            - **status=PENDING**: 승인 대기중인 신청자 명단 (수락/거절 필요)
            - **status=APPROVED**: 참가가 확정된 인원 명단
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HostPartyApplicationResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "content": [
                                        {
                                            "applicationId": 50,
                                            "status": "PENDING",
                                            "answerText": "보드게임 경력 3년입니다! 룰 설명 가능해요.",
                                            "appliedAt": "2025-11-20T10:00:00",
                                            "userId": 12,
                                            "name": "김신청",
                                            "profileImageUrl": "https://example.com/user12.jpg",
                                            "gender": "MALE",
                                            "birthdate": "1995-05-05",
                                            "mbti": "ENFP",
                                            "region": "서울"
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
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 파티가 아님)"),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음")
    })
    @GetMapping("/parties/{partyId}/applications")
    public ResponseEntity<ApiResult<Page<HostPartyApplicationResponse>>> getPartyApplications(
            Authentication authentication,
            @PathVariable Long partyId,
            @RequestParam ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return ResponseEntity.ok(ApiResult.onSuccess(hostService.getPartyApplications(userId, partyId, status, pageable)));
    }

    /**
     * 참가 신청 승인/거절 API (상태 변경만 수행)
     */
    @Operation(
            summary = "참가 신청 승인/거절",
            description = "신청 상태만 변경합니다. 알림은 별도 API로 발송해야 합니다." +
                    "파티에 대한 정보는 applicationId 가 가지고 있습니다." +
                    "request body 는 APPROVED, REJECTED, PENDING 중에 선택할 수 있습니다.",
            tags = {"Host"}
    )
    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApiResult<Void>> updateApplicationStatus(
            Authentication authentication,
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        hostService.updateApplicationStatus(hostUserId, applicationId, request.getStatus());
        return ResponseEntity.ok(ApiResult.onSuccess());
    }
    /**
     * [신규] 참가 신청 일괄 승인/거절 API (체크박스 처리용)
     */
    @Operation(
            summary = "참가 신청 일괄 승인/거절 (다건)",
            description = """
            여러 개의 신청서를 선택하여(체크박스 등) 한 번에 승인하거나 거절합니다.
            하나라도 본인의 파티 신청이 아니면 에러가 발생합니다.
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일괄 처리 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "403", description = "권한 없는 신청 포함됨")
    })
    @PatchMapping("/applications/status")
    public ResponseEntity<ApiResult<Void>> updateBulkApplicationStatus(
            Authentication authentication,
            @Valid @RequestBody BulkUpdateApplicationStatusRequest request
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        hostService.updateBulkApplicationStatus(hostUserId, request);
        return ResponseEntity.ok(ApiResult.onSuccess());
    }
    /**
     * 참가 결과 일괄 알림 발송 API
     * [POST] /api/v1/host/parties/{partyId}/notifications/result
     */
    @Operation(
            summary = "참가 결과 일괄 알림 발송",
            description = """
            해당 파티의 **승인(APPROVED)** 및 **거절(REJECTED)**된 신청자들에게 결과 알림톡을 일괄 전송합니다.
            
            - **사용 시점**: 호스트가 신청자들의 승인/거절 처리를 마친 후(또는 중간중간) 결과를 통보하고 싶을 때 사용합니다.
            - **대상**: 상태가 `APPROVED` 또는 `REJECTED`인 모든 신청자.
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "발송 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "알림을 보낼 대상이 없음 (모두 PENDING 상태이거나 신청자가 없음)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": false,
                                "code": "COMMON4001",
                                "message": "유효하지 않은 입력 값입니다.",
                                "result": "알림을 보낼 대상이 없습니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 파티가 아님)"),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음")
    })
    @PostMapping("/parties/{partyId}/notifications/result")
    public ResponseEntity<ApiResult<Void>> sendApplicationResultNotifications(
            Authentication authentication,
            @PathVariable Long partyId
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        hostService.sendApplicationResultNotifications(hostUserId, partyId);
        return ResponseEntity.ok(ApiResult.onSuccess());
    }

    /**
     * AI 매칭 실행 API (매칭만 수행)
     * [POST] /api/v1/host/parties/{partyId}/match
     */
    @Operation(
            summary = "AI 매칭 실행",
            description = """
            특정 파티의 **'승인된(APPROVED)'** 참가자들을 대상으로 AI 그룹 매칭을 실행합니다.
            이 API는 매칭 결과(조 편성)를 DB에 저장하기만 하며, 알림은 발송하지 않습니다.
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매칭 성공"),
            @ApiResponse(responseCode = "400", description = "매칭할 참가자가 없음"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 파티 아님)")
    })
    @PostMapping("/parties/{partyId}/match")
    public ResponseEntity<ApiResult<Void>> runAiMatching(
            Authentication authentication,
            @Parameter(description = "매칭을 실행할 파티 ID", required = true)
            @PathVariable Long partyId
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        aiMatchingService.runAiMatching(hostUserId, partyId);
        return ResponseEntity.ok(ApiResult.onSuccess());
    }
    /**
     * [신규] AI 매칭 결과 조회 API
     */
    @Operation(
            summary = "AI 매칭 결과 조회",
            description = "실행된 AI 매칭 결과(조 편성 현황)를 조회합니다. 호스트가 알림 발송 전 확인용으로 사용합니다.",
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiMatchingResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": [
                                    {
                                        "groupId": 1,
                                        "groupName": "AI 추천 1조",
                                        "matchingReason": "MBTI 성향 유사",
                                        "members": [
                                            {
                                                "userId": 7,
                                                "name": "김철수",
                                                "profileImageUrl": "http://img...",
                                                "mbti": "ENTP"
                                            },
                                            {
                                                "userId": 8,
                                                "name": "이영희",
                                                "profileImageUrl": "http://img...",
                                                "mbti": "INTP"
                                            }
                                        ]
                                    }
                                ]
                            }
                            """)
                    )
            )
    })
    @GetMapping("/parties/{partyId}/match")
    public ResponseEntity<ApiResult<List<AiMatchingResponse>>> getMatchingResults(
            Authentication authentication,
            @PathVariable Long partyId
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        List<AiMatchingResponse> results = aiMatchingService.getMatchingResults(hostUserId, partyId);
        return ResponseEntity.ok(ApiResult.onSuccess(results));
    }
    /**
     * 파티 확정 및 알림 발송 API
     * [POST] /api/v1/host/parties/{partyId}/finalize
     */
    @Operation(
            summary = "파티 확정 및 결과 알림 발송",
            description = """
            파티 상태를 **SCHEDULED(확정)**로 변경하고, 참가자들에게 **결과 알림톡**을 일괄 발송합니다.
            - 승인자: 조 편성 결과 + 매칭 사유 전송 (AI 매칭이 선행되어야 함)
            - 거절자: 탈락 안내 전송
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 및 알림 발송 성공"),
            @ApiResponse(responseCode = "400", description = "매칭 결과가 없거나 대상자가 없음"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/parties/{partyId}/finalize")
    public ResponseEntity<ApiResult<Void>> finalizeParty(
            Authentication authentication,
            @Parameter(description = "확정할 파티 ID", required = true)
            @PathVariable Long partyId
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        aiMatchingService.finalizePartyAndSendNotification(hostUserId, partyId);
        return ResponseEntity.ok(ApiResult.onSuccess());
    }

    /**
     * [신규] 후기 작성 요청 일괄 알림 발송 API
     * [POST] /api/v1/host/parties/{partyId}/notifications/review
     */
    @Operation(
            summary = "후기 작성 요청 알림 발송",
            description = """
            파티가 종료된 후, 호스트가 참석자들에게 후기 작성을 요청하는 알림톡을 일괄 발송합니다.
            **대상:** 해당 파티에 '참석 완료(ATTENDED)' 상태인 모든 유저
            """,
            tags = {"Host"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "참석자가 없음"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 파티 아님)")
    })
    @PostMapping("/parties/{partyId}/notifications/review")
    public ResponseEntity<ApiResult<Void>> sendReviewRequestNotifications(
            Authentication authentication,
            @Parameter(description = "파티 ID", required = true)
            @PathVariable Long partyId
    ) {
        Long hostUserId = Long.parseLong(authentication.getName());
        hostService.sendReviewRequestNotifications(hostUserId, partyId);
        return ResponseEntity.ok(ApiResult.onSuccess());
    }
    // [Helper] Pageable 생성 메서드
    private Pageable createPageable(int page, int size, String sort) {
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isEmpty()) {
            try {
                String[] sortParams = sort.split(",");
                Sort.Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")) ?
                        Sort.Direction.DESC : Sort.Direction.ASC;
                return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
            } catch (Exception e) {
                return PageRequest.of(page, size, defaultSort);
            }
        }
        return PageRequest.of(page, size, defaultSort);
    }
}