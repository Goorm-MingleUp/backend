package com.mingleup.backend.domain.wishlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WishlistResponse {
    private String status;    // "added" or "removed"
    private int wishCount;    // 해당 파티의 총 찜 개수
}
