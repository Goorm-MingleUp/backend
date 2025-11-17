package com.mingleup.backend.domain.wishlist.dto;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [GET] /api/v1/wishlists/me 응답 DTO
 */
@Getter
@Builder
public class MyWishlistResponse {

    // Wishlist 정보
    private Long wishlistId;
    private LocalDateTime wishlistedAt; // 찜한 시간

    // Party 정보
    private Long partyId;
    private String partyTitle;
    private String partyImageUrl;
    private LocalDateTime partyDatetime;
    private String partyLocationName;
    private int entryFee;

    /**
     * Wishlist 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     * (N+1 문제 발생 지점)
     */
    public static MyWishlistResponse from(Wishlist wishlist) {
        Party party = wishlist.getParty(); // Lazy Loading (N+1)

        return MyWishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .wishlistedAt(wishlist.getCreatedAt())
                .partyId(party.getId())
                .partyTitle(party.getTitle())
                .partyImageUrl(party.getPartyImageUrl())
                .partyDatetime(party.getPartyDatetime())
                .partyLocationName(party.getLocationName())
                .entryFee(party.getEntryFee())
                .build();
    }
}