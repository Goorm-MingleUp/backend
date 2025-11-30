package com.mingleup.backend.domain.application.controller;

import com.mingleup.backend.domain.application.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // [추가]
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Application", description = "모임 신청 내역 API")
@RestController
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
    @GetMapping("/api/v1/applications/me")
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

    @Operation(
            summary = "파티 신청",
            description = """
                특정 파티에 참가 신청합니다.
                - 파티에 등록된 단일 호스트 질문에 대한 자유서술( answer_text )을 함께 제출합니다.
                - FCFS(선착순)일 경우 자동 승인, APPROVAL(승인제)일 경우 PENDING 상태로 생성됩니다.
                - 중복 신청 불가
                """
    )
    @PostMapping("/api/v1/parties/{partyId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public PartyApplicationResponse apply(
            @PathVariable Long partyId,
            @RequestAttribute("userId") Long userId,
            @RequestBody PartyApplicationRequest request
    ) {
        return partyApplicationService.apply(partyId, userId, request);
    }

    @Operation(
            summary = "파티 신청 취소",
            description = """
                사용자가 자신이 신청한 파티를 취소합니다.
                - 이미 종료된 파티는 취소할 수 없습니다.
                """
    )
    @DeleteMapping("/api/v1/parties/{partyId}/applications")
    public ResponseEntity<PartyApplicationCancelResponse> cancelApplication(
            @PathVariable Long partyId,
            @RequestAttribute("userId") Long userId
    ) {
        PartyApplicationCancelResponse response = partyApplicationService.cancelApplication(partyId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "파티 참석자 전체 조회",
            description = "특정 파티에 승인된 참석자 전체 목록을 조회합니다."
    )
    @GetMapping("/api/v1/parties/{partyId}/attendees")
    public ResponseEntity<ApiResult<PartyAttendeesResponse>> getAttendees(
            @PathVariable Long partyId
    ) {
        PartyAttendeesResponse response = partyApplicationService.getPartyAttendees(partyId);
        return ResponseEntity.ok(ApiResult.onSuccess(response));
    }

}