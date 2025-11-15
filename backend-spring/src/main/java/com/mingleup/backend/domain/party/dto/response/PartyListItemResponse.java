package com.mingleup.backend.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "파티 리스트 아이템 응답 DTO")
public record PartyListItemResponse(

        @Schema(example = "1")
        Long party_id,

        @Schema(example = "해피 할로윈 인원 구합니다.")
        String title,

        @Schema(example = "https://cdn.mingleup.party/party1.png")
        String party_image_url,

        @Schema(example = "동작구")
        String location_name,

        @Schema(example = "파티")
        String category,

        @Schema(example = "2025-11-20T18:00:00")
        LocalDateTime party_datetime,

        @Schema(example = "recruiting")
        String status,

        @Schema(example = "15")
        Integer wish_count
) {}
