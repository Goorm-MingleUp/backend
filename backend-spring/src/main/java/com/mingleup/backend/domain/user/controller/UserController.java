package com.mingleup.backend.domain.user.controller;

import com.mingleup.backend.domain.user.dto.UserInfoResponse;
import com.mingleup.backend.domain.user.dto.UpdateUserInfoRequest;

import com.mingleup.backend.domain.user.dto.UserProfileResponse; // [추가]
import com.mingleup.backend.domain.user.dto.UserReviewResponse; // [추가]
import com.mingleup.backend.domain.user.service.UserService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter; // [추가]

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.data.web.PageableDefault; // [추가]

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.List; // [추가]


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") // Swagger에서 JWT 인증 헤더를 요구하도록 표시
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회 API (기존)
     * [GET] /api/v1/users/me
     */
    @Operation(
            summary = "내 정보 조회 (본인)",
            description = """
            JWT 토큰 기반으로 현재 로그인한 사용자의 **본인** 프로필 정보를 조회합니다.
            항상 모든 정보가 반환됩니다. (참가자라도 평점 포함)
            
            (참고: `GET /api/v1/users/{userId}` API는 타인 조회용이며 접근 제어 로직이 다릅니다.)

            """,
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserInfoResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {

                                    "id": 1,
                                    "name": "김코딩",
                                    "email": "xorud315@naver.com",
                                    "profileImageUrl": "https://example.com/images/profile.jpg",
                                    "gender": "MALE",
                                    "birthdate": "1995-10-21",
                                    "role": "PARTICIPANT",
                                    "region": "서울",
                                    "mbti": "ISTJ",
                                    "hobbies": ["코딩", "등산", "맛집탐방"],
                                    "idealTypeHobbies": ["보드게임", "산책"],
                                    "hostNickname": "코딩호스트"
                                }
                            }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - JWT 토큰이 없거나 유효하지 않음",
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
                    description = "토큰은 유효하나, 해당 사용자가 DB에 존재하지 않음",
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
    public ResponseEntity<ApiResult<UserInfoResponse>> getMyInfo(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserInfoResponse myInfo = userService.getMyInfo(userId);

        return ResponseEntity.ok(ApiResult.onSuccess(myInfo));
    }

    /**
     * 내 정보 수정 API
     * [PUT] /api/v1/users/me
     */
    @Operation(
            summary = "내 정보 수정 (추가 정보 기입)",
            description = """
            JWT 토큰 기반으로 현재 로그인한 사용자의 추가 정보를 수정(업데이트)합니다.
            신규 가입자가 '회원 정보 기입' 폼을 제출할 때 사용됩니다.
            """,
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 정보 수정 성공",
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
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없습니다.")
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResult<Void>> updateMyInfo(
            Authentication authentication,
            @RequestBody UpdateUserInfoRequest request
    ) {
        Long userId = Long.parseLong(authentication.getName());
        userService.updateMyInfo(userId, request);

        return ResponseEntity.ok(ApiResult.onSuccess()); // result: null
    }

    // --- [API 4] ---
    /**
     * (신규) 타인 프로필 조회 API
     * [GET] /api/v1/users/{userId}
     */
    @Operation(
            summary = "유저 프로필 조회", // [수정]
            description = """
            `{userId}`에 해당하는 사용자의 프로필 정보를 조회합니다.
            
            - **호스트 프로필**: 누구나 조회 가능 (평점 포함)
            - **참가자 프로필**:
                - 본인 조회 시: 조회 가능 (단, `avgRating`은 `null`로 반환)
                - 호스트가 신청자 조회 시: 조회 가능 (평점 포함)
                - 그 외: 403 Forbidden
            
            (참고: 본인 정보의 모든 필드를 보려면 `GET /api/v1/users/me`를 사용하세요.)
            """, // [수정]
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class),
                            examples = {
                                    @ExampleObject(name = "호스트 조회 (평점 포함)", value = """
                                    {
                                      "success": true,
                                      "code": "COMMON200",
                                      "message": "성공입니다.",
                                      "result": {
                                        "userId": 1,
                                        "name": "김태경",
                                        "profileImageUrl": "http://k.kakaocdn.net/dn/6YvN9/dJMcabCzn9r/JoguXtLFRuaPjWSPRjuabK/img_640x640.jpg",
                                        "avgRating": 0
                                      }
                                    }
                                    """), // [수정]
                                    @ExampleObject(name = "참가자가 본인 조회 (평점 null)", value = """
                                    {
                                      "success": true,
                                      "code": "COMMON200",
                                      "message": "성공입니다.",
                                      "result": {
                                        "userId": 1,
                                        "name": "김태경",
                                        "profileImageUrl": "http://k.kakaocdn.net/dn/6YvN9/dJMcabCzn9r/JoguXtLFRuaPjWSPRjuabK/img_640x640.jpg"
                                      }
                                    }
                                    """) // [수정]
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음 (예: 참가자가 다른 참가자 조회)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": false,
                                "code": "AUTH4004",
                                "message": "인가에 실패했습니다.",
                                "result": "이 프로필을 조회할 권한이 없습니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없습니다.")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<UserProfileResponse>> getUserProfile(
            @Parameter(description = "조회할 대상 사용자의 ID", required = true)
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        UserProfileResponse userProfile = userService.getUserProfile(userId, currentUserId);

        return ResponseEntity.ok(ApiResult.onSuccess(userProfile));
    }

    // --- [API 5] ---
    /**
     * (신규) 타인 후기 목록 조회 API
     * [GET] /api/v1/users/{userId}/reviews
     */
    @Operation(
            summary = "유저 후기 목록 조회", // [수정]
            description = """
            `{userId}`에 해당하는 사용자가 받은 후기 목록을 조회합니다. (v2 접근 제어 로직 적용)
            
            - **호스트 후기**: 누구나 조회 가능
            - **참가자 후기**:
                - 본인 조회 시: 403 Forbidden (조회 불가)
                - 호스트가 신청자 조회 시: 조회 가능
                - 그 외: 403 Forbidden
                
            **페이징 지원:** `page` (페이지 번호, 0부터 시작), `size` (페이지 크기) 쿼리 파라미터를 사용할 수 있습니다.
            (예: `/api/v1/users/1/reviews?page=0&size=5`)
            """, // [수정]
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "후기 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "content": [
                                        {
                                            "reviewId": 1,
                                            "reviewerId": 2,
                                            "reviewerName": "유저2",
                                            "rating": 5,
                                            "comment": "친절하고 분위기 좋았어요!",
                                            "createdAt": "2025-11-17T01:54:04"
                                        },
                                        {
                                            "reviewId": 2,
                                            "reviewerId": 3,
                                            "reviewerName": "유저3",
                                            "rating": 4,
                                            "comment": "보드게임 다양하고 재밌었습니다.",
                                            "createdAt": "2025-11-17T01:54:04"
                                        }
                                    ],
                                    "pageable": { "pageNumber": 0, "pageSize": 10, ... },
                                    "totalPages": 1,
                                    "totalElements": 2,
                                    "last": true,
                                    ...
                                }
                            }
                        """) // [수정]
                    ))
            , // [수정] 중복된 괄호 ')' 삭제
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음 (예: 참가자가 본인 또는 다른 참가자 후기 조회)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "success": false,
                                "code": "AUTH4004",
                                "message": "인가에 실패했습니다.",
                                "result": "이 후기 목록을 조회할 권한이 없습니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없습니다.")
    })
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<ApiResult<Page<UserReviewResponse>>> getUserReviews( // [수정] List -> Page
                                                                               @Parameter(description = "후기 목록을 조회할 대상 사용자의 ID", required = true)
                                                                               @PathVariable Long userId,
                                                                               Authentication authentication,
                                                                               @Parameter(description = "페이징 설정 (page, size, sort)") // [추가]
                                                                               @PageableDefault(size = 10, page = 0) Pageable pageable // [추가]
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        // [수정] pageable 파라미터 전달
        Page<UserReviewResponse> reviews = userService.getUserReviews(userId, currentUserId, pageable);

        return ResponseEntity.ok(ApiResult.onSuccess(reviews));
    }
}