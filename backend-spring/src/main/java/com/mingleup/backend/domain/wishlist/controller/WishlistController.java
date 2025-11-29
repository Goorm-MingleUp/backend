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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 내 찜한 파티 목록 조회 API
     * [GET] /api/v1/wishlists/me
     */
    @Operation(summary = "내 찜 목록 조회", description = "내가 찜한 모임들의 목록을 최신순으로 조회합니다.")
    @GetMapping("/wishlists/me")
    public ResponseEntity<ApiResult<Page<MyWishlistResponse>>> getMyWishlistedParties(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long currentUserId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(ApiResult.onSuccess(wishlistService.getMyWishlistedParties(currentUserId, pageable)));
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