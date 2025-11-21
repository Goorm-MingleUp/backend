package com.mingleup.backend.domain.party.dto.response;

import com.mingleup.backend.domain.party.domain.Party;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 파티 생성 응답 DTO
 */
@Schema(description = "파티 생성 응답 DTO")
@Builder
public record PartyCreateResponse(

        @Schema(description = "생성된 파티 ID", example = "1")
        Long partyId,

        @Schema(description = "파티 제목", example = "해피 할로윈 인원 구합니다.")
        String title,

        @Schema(description = "카테고리", example = "파티")
        String category,

        @Schema(description = "파티 이미지 URL", example = "https://cdn.mingleup.party/party1.png")
        String partyImageUrl

) {
        public static PartyCreateResponse from(Party party) {
                return PartyCreateResponse.builder()
                        .partyId(party.getId())
                        .title(party.getTitle())
                        .category(party.getCategory())
                        .partyImageUrl(party.getPartyImageUrl())
                        .build();
        }
}
