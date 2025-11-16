package com.mingleup.backend.domain.wishlist.controller;

import com.mingleup.backend.domain.wishlist.dto.MyWishlistResponse;
import com.mingleup.backend.domain.wishlist.service.WishlistService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content; // [추가]
import io.swagger.v3.oas.annotations.media.ExampleObject; // [추가]
import io.swagger.v3.oas.annotations.media.Schema; // [추가]
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Wishlist", description = "찜하기 API")
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 내 찜한 파티 목록 조회 API
     * [GET] /api/v1/wishlists/me
     */
    @Operation(
            summary = "내 찜한 파티 목록 조회",
            description = "현재 로그인한 사용자가 찜(wishlist)한 모든 모임 목록을 조회합니다.",
            tags = {"Wishlist"}
    )
    @ApiResponses({
            // --- [수정] 200, 401, 404 응답 예시 추가 ---
            @ApiResponse(
                    responseCode = "200",
                    description = "찜 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MyWishlistResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "success": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": [
                                    {
                                        "wishlistId": 1,
                                        "wishlistedAt": "2025-11-17T11:00:00",
                                        "partyId": 6,
                                        "partyTitle": "호스트3(요가언니)의 베이킹 클래스",
                                        "partyImageUrl": "https://example.com/img/party6.jpg",
                                        "partyDatetime": "2025-12-15T15:00:00",
                                        "partyLocationName": "연남동 베이킹 스튜디오",
                                        "entryFee": 30000
                                    }
                                ]
                            }
                            """)
                    )
            ),
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
            // --- [수정] 끝 ---
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResult<List<MyWishlistResponse>>> getMyWishlistedParties(
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        List<MyWishlistResponse> myWishlists = wishlistService.getMyWishlistedParties(currentUserId);
        return ResponseEntity.ok(ApiResult.onSuccess(myWishlists));
    }

    // (TODO: 찜하기(POST), 찜 취소(DELETE) API 추가)
}