package com.mingleup.backend.domain.party.dto.response;

import java.util.List;

public record PartyCategoryResponse(
        String category,
        List<String> subCategories
) { }
