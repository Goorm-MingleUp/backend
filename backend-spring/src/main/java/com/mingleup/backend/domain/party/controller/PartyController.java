package com.mingleup.backend.domain.party.controller;

import com.mingleup.backend.domain.party.dto.request.PartyCreateRequest;
import com.mingleup.backend.domain.party.dto.request.PartyUpdateRequest;
import com.mingleup.backend.domain.party.dto.response.PartyCreateResponse;
import com.mingleup.backend.domain.party.dto.response.PartyDetailResponse;
import com.mingleup.backend.domain.party.dto.response.PartyListResponse;
import com.mingleup.backend.domain.party.service.PartyService;
import com.mingleup.backend.domain.wishlist.dto.response.WishlistResponse;
import com.mingleup.backend.domain.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parties")
@Tag(name = "Party API", description = "파티 관련 API")
public class PartyController {

    private final PartyService partyService;
    private final WishlistService wishlistService;

    @Operation(
            summary = "파티 리스트 조회",
            description = """
                파티 목록을 조회합니다.
                - 검색(search), 상태(status), 카테고리(category), 정렬(sort_by) 지원
                - 페이지네이션(page, limit)
                """
    )
    @GetMapping
    public PartyListResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(name = "sort_by", required = false) String sortBy
    ) {
        return partyService.getParties(page, limit, search, status, category, sortBy);
    }

    @Operation(
            summary = "파티 상세 조회",
            description = "단일 파티 상세 정보를 조회합니다."
    )
    @GetMapping("/{partyId}")
    public PartyDetailResponse detail(@PathVariable Long partyId) {
        return partyService.getParty(partyId);
    }

    @Operation(
            summary = "파티 생성",
            description = """
                새로운 파티를 생성합니다.
                - 인증된 사용자(Host)만 생성 가능
                """
    )
    @PostMapping
    public PartyCreateResponse create(
            @RequestAttribute(value = "userId", required = false) Long id,
            @RequestBody PartyCreateRequest req
    ) {
        if (id == null) {
            id = 1L;
        }
        return partyService.createParty(id, req);
    }

    @Operation(
            summary = "파티 수정",
            description = """
                기존 파티 정보를 수정합니다.
                - 해당 파티의 호스트만 수정 가능합니다.
                """
    )
//    @PutMapping("/{partyId}")
//    public void update(
//            @PathVariable Long partyId,
//            //@RequestAttribute("userId") Long userId,
//            @RequestParam("userId") Long userId,
//            @RequestBody PartyUpdateRequest req
//    ) {
//        partyService.updateParty(partyId, userId, req);
//    }

    @PutMapping("/{partyId}")
    public ResponseEntity<Void> update(
            @PathVariable Long partyId,
            //@RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Long id,
            @RequestBody PartyUpdateRequest req
    ) {
        if (id == null) {
            id = 1L;
        }
        partyService.updateParty(partyId, id, req);
        return ResponseEntity.noContent().build(); // 204
    }


    @Operation(
            summary = "파티 찜하기",
            description = "해당 파티를 찜 목록에 추가합니다."
    )
    @PostMapping("/{partyId}/wishlist")
    public WishlistResponse addWish(
            @PathVariable Long partyId,
            @RequestAttribute(value = "userId", required = false) Long id
    ) {
        if (id == null) {
            id = 1L;
        }
        return wishlistService.add(partyId, id); // 혹은 기존 코드
    }

    @Operation(
            summary = "파티 찜 취소",
            description = "해당 파티를 찜 목록에서 제거합니다."
    )
    @DeleteMapping("/{partyId}/wishlist")
    public WishlistResponse removeWish(
            @PathVariable Long partyId,
            @RequestAttribute(value = "userId", required = false) Long id
    ) {
        if (id == null) {
            id = 1L;
        }
        return wishlistService.remove(partyId, id);
    }


}
