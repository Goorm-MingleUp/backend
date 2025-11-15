package com.mingleup.backend.domain.wishlist.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record WishlistResponse(
        String status,
        int wish_count
) {}
