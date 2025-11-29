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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wishlist", description = "찜(위시리스트) API")
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 내 찜한 파티 목록 조회 API
     */
    @Operation(
            summary = "내 찜 목록 조회",
            description = """
            내가 찜한 모임들의 목록을 최신순(찜한 날짜 기준)으로 조회합니다.
            """,
            tags = {"Wishlist"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
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
                                            "wishlistId": 5,
                                            "wishlistedAt": "2025-11-20T12:00:00",
                                            "partyId": 13,
                                            "partyTitle": "비건 베이킹 클래스",
                                            "partyImageUrl": "https://img.url/baking.jpg",
                                            "partyDatetime": "2025-12-18T14:00:00",
                                            "partyLocationName": "망원동 키친",
                                            "entryFee": 45000
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
    @GetMapping("/me")
    public ResponseEntity<ApiResult<Page<MyWishlistResponse>>> getMyWishlistedParties(
            Authentication authentication,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 (예: createdAt,desc)") @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long currentUserId = Long.parseLong(authentication.getName());
        Page<MyWishlistResponse> myWishlists = wishlistService.getMyWishlistedParties(currentUserId, pageable);
        return ResponseEntity.ok(ApiResult.onSuccess(myWishlists));
    }

    @Operation(summary = "파티 찜하기", description = "지정된 파티를 찜 목록에 추가합니다.")
    @PostMapping("/{partyId}")
    public WishlistResponse addWishlist(
            @PathVariable Long partyId,
            @RequestAttribute("userId") Long userId
    ) {
        return wishlistService.add(partyId, userId);
    }

    @Operation(summary = "파티 찜 취소", description = "지정된 파티를 찜 목록에서 제거합니다.")
    @DeleteMapping("/{partyId}")
    public WishlistResponse removeWishlist(
            @PathVariable Long partyId,
            @RequestAttribute("userId") Long userId
    ) {
        return wishlistService.remove(partyId, userId);
    }
}