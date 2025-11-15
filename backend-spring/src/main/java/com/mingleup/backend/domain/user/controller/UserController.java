package com.mingleup.backend.domain.user.controller;

import com.mingleup.backend.domain.user.dto.UserInfoResponse;
import com.mingleup.backend.domain.user.dto.UpdateUserInfoRequest;
import com.mingleup.backend.domain.user.service.UserService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") // Swagger에서 JWT 인증 헤더를 요구하도록 표시
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회 API
     * [GET] /api/v1/users/me
     */
    @Operation(
            summary = "내 정보 조회",
            description = """
            JWT 토큰 기반으로 현재 로그인한 사용자의 프로필 정보를 조회합니다.  
            토큰 내 userId를 이용하여 `UserService`에서 내 정보(`UserInfoResponse`)를 반환합니다.
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
                                    "userId": 1,
                                    "email": "xorud315@naver.com",
                                    "name": "김코딩",
                                    "gender": "MALE",
                                    "birthdate": "1995-10-21",
                                    "region": "서울",
                                    "mbti": "ISTJ",
                                    "hobbies": ["코딩", "등산", "맛집탐방"],
                                    "idealTypeHobbies": ["보드게임", "산책"],
                                    "profileImageUrl": "https://example.com/images/profile.jpg"
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
                                "message": "성공입니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - JWT 토큰이 없거나 유효하지 않음"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "토큰은 유효하나, 해당 사용자가 DB에 존재하지 않음"
            )
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
}