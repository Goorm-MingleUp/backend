package com.mingleup.backend.domain.party.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PartyUpdateRequest(
        String title,
        String description,
        String guidelines,
        String category,
        List<String> sub_category,
        LocalDateTime party_datetime,
        String location_name,
        String location_address,
        Integer min_participants,
        Integer max_participants,
        String recruitment_method,
        Integer entry_fee,
        List<String> tags,
        List<String> host_questions,
        String party_image_url
) {}
