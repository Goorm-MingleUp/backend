package com.mingleup.backend.domain.user.controller;

import com.mingleup.backend.domain.user.dto.UpdateUserInfoRequest;
import com.mingleup.backend.domain.user.dto.UserInfoResponse;
import com.mingleup.backend.domain.user.dto.UserProfileResponse;
import com.mingleup.backend.domain.user.dto.UserReviewResponse;
import com.mingleup.backend.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저 정보/프로필 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회 API
     */
    @Operation(
            summary = "내 정보 조회 (마이페이지)",
            description = """
            현재 로그인한 사용자의 상세 프로필 정보를 조회합니다.
            본인 정보이므로 평점, 이메일 등 모든 민감 정보가 포함됩니다.
            """,
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
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
                                    "email": "test@example.com",
                                    "profileImageUrl": "https://k.kakaocdn.net/dn/img.jpg",
                                    "gender": "MALE",
                                    "birthdate": "1995-10-21",
                                    "role": "PARTICIPANT",
                                    "region": "서울",
                                    "mbti": "ISTJ",
                                    "hobbies": ["코딩", "독서"],
                                    "idealTypeHobbies": ["산책", "맛집탐방"],
                                    "hostNickname": "코딩왕"
                                }
                            }
                            """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserInfoResponse>> getMyInfo(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserInfoResponse myInfo = userService.getMyInfo(userId);
        return ResponseEntity.ok(ApiResult.onSuccess(myInfo));
    }

    /**
     * 내 정보 수정 API
     */
    @Operation(
            summary = "내 정보 수정 (추가 정보 기입)",
            description = """
            회원가입 후 혹은 마이페이지에서 추가 정보(지역, MBTI, 취미 등)를 수정합니다.
            입력하지 않은 필드는 null로 보내면 기존 값이 유지되지 않고 null로 덮어씌워질 수 있으니 주의하세요(구현에 따라 다름).
            """,
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
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
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResult<Void>> updateMyInfo(
            Authentication authentication,
            @RequestBody UpdateUserInfoRequest request
    ) {
        Long userId = Long.parseLong(authentication.getName());
        userService.updateMyInfo(userId, request);
        return ResponseEntity.ok(ApiResult.onSuccess());
    }

    /**
     * 타인 프로필 조회 API
     */
    @Operation(
            summary = "타인 프로필 조회",
            description = """
            특정 사용자(`userId`)의 프로필을 조회합니다.
            
            **접근 권한 규칙:**
            1. **호스트 프로필**: 누구나 조회 가능 (평점 포함)
            2. **참가자 프로필**:
               - 본인이 조회: 조회 가능 (단, 평점은 null)
               - 호스트가 신청자를 조회: 조회 가능 (평점 포함)
               - 그 외: 403 Forbidden
            """,
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class),
                            examples = {
                                    @ExampleObject(name = "호스트 조회 (평점 O)", value = """
                                    {
                                        "success": true,
                                        "code": "COMMON200",
                                        "message": "성공입니다.",
                                        "result": {
                                            "userId": 6,
                                            "name": "박호스트",
                                            "profileImageUrl": "https://img.url/host.jpg",
                                            "hostIntro": "안녕하세요",
                                            "avgRating": 4.5,
                                            "hostNickname": "친절한호스트"
                                        }
                                    }
                                    """),
                                    @ExampleObject(name = "참가자 본인 조회 (평점 X)", value = """
                                    {
                                        "success": true,
                                        "code": "COMMON200",
                                        "message": "성공입니다.",
                                        "result": {
                                            "userId": 1,
                                            "name": "김참가",
                                            "profileImageUrl": "https://img.url/me.jpg",
                                            "hostIntro": null,
                                            "avgRating": null,
                                            "hostNickname": null
                                        }
                                    }
                                    """)
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음 (참가자가 다른 참가자 조회 등)"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<UserProfileResponse>> getUserProfile(
            @Parameter(description = "조회할 대상 ID") @PathVariable Long userId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        UserProfileResponse userProfile = userService.getUserProfile(userId, currentUserId);
        return ResponseEntity.ok(ApiResult.onSuccess(userProfile));
    }

    /**
     * 타인 후기 목록 조회 API
     */
    @Operation(
            summary = "타인 후기 목록 조회",
            description = """
            특정 사용자가 받은 후기 목록을 페이징하여 조회합니다.
            프로필 조회와 동일한 접근 권한 규칙이 적용됩니다.
            """,
            tags = {"User"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserReviewResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "content": [
                                        {
                                            "reviewId": 10,
                                            "reviewerId": 7,
                                            "reviewerName": "유저7",
                                            "rating": 5,
                                            "comment": "친절하십니다!",
                                            "createdAt": "2025-11-20T10:00:00"
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
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<ApiResult<Page<UserReviewResponse>>> getUserReviews(
            @PathVariable Long userId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long currentUserId = Long.parseLong(authentication.getName());
        Page<UserReviewResponse> reviews = userService.getUserReviews(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResult.onSuccess(reviews));
    }
}