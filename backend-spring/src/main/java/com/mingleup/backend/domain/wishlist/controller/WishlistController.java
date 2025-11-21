package com.mingleup.backend.domain.wishlist.controller;

import com.mingleup.backend.domain.wishlist.dto.MyWishlistResponse;
import com.mingleup.backend.domain.wishlist.dto.response.WishlistResponse;
import com.mingleup.backend.domain.wishlist.service.WishlistService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wishlist", description = "찜하기 API")
@RestController
@RequestMapping
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
            description = """
            현재 로그인한 사용자가 찜(wishlist)한 모든 모임 목록을 조회합니다.
            
            **페이징 지원:** `page` (페이지 번호, 0부터 시작), `size` (페이지 크기) 쿼리 파라미터를 사용할 수 있습니다.
            (예: `/api/v1/wishlists/me?page=0&size=10`)
            """,
            tags = {"Wishlist"}
    )
    @ApiResponses({
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
                                "result": {
                                    "content": [
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
                                    ],
                                    "pageable": {
                                        "pageNumber": 0,
                                        "pageSize": 3,
                                        "sort": { "sorted": false, "unsorted": true, "empty": true },
                                        "offset": 0,
                                        "paged": true,
                                        "unpaged": false
                                    },
                                    "totalPages": 1,
                                    "totalElements": 1,
                                    "last": true,
                                    "size": 10,
                                    "number": 0,
                                    "sort": { "sorted": false, "unsorted": true, "empty": true },
                                    "numberOfElements": 1,
                                    "first": true,
                                    "empty": false
                                }
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
    })
    @GetMapping("/api/v1/wishlists/me")
    public ResponseEntity<ApiResult<Page<MyWishlistResponse>>> getMyWishlistedParties(
           Authentication authentication,
           @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
               @RequestParam(defaultValue = "0") int page,

           @Parameter(description = "페이지 크기", example = "10")
               @RequestParam(defaultValue = "10") int size,

           @Parameter(description = "정렬 기준 (예: wishlistedAt,desc)", example = "wishlistedAt,desc")
               @RequestParam(required = false) String sort
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Long currentUserId = Long.parseLong(authentication.getName());
        Page<MyWishlistResponse> myWishlists =
                wishlistService.getMyWishlistedParties(currentUserId, pageable);

        return ResponseEntity.ok(ApiResult.onSuccess(myWishlists));
    }

    // (TODO: 찜하기(POST), 찜 취소(DELETE) API 추가)

    @Operation(
            summary = "파티 찜하기",
            description = "해당 파티를 찜 목록에 추가합니다."
    )
    @PostMapping("/api/v1/{partyId}/wishlist")
    public WishlistResponse addWish(
            @PathVariable Long partyId,
            @RequestAttribute("userId") Long userId
    ) {
        return wishlistService.add(partyId, userId);
    }

    @Operation(
            summary = "파티 찜 취소",
            description = "해당 파티를 찜 목록에서 제거합니다."
    )
    @DeleteMapping("/api/v1/{partyId}/wishlist")
    public WishlistResponse removeWish(
            @PathVariable Long partyId,
            @RequestAttribute("userId") Long userId
    ) {
        return wishlistService.remove(partyId, userId);
    }
}