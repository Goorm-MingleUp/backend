package com.mingleup.backend.domain.party.dto.response;

import com.mingleup.backend.domain.party.domain.Party;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "파티 목록 응답 DTO")
@Builder
public record PartyListResponse(

        @Schema(description = "총 파티 수", example = "25")
        int totalCount,

        @Schema(description = "현재 페이지 번호", example = "1")
        int currentPage,

        @Schema(description = "파티 요약 목록")
        List<PartySummary> result
) {

        @Schema(description = "파티 요약 정보 DTO")
        @Builder
        public record PartySummary(
                @Schema(description = "파티 ID", example = "1")
                Long partyId,

                @Schema(description = "제목", example = "홍대 와인 모임")
                String title,

                @Schema(description = "카테고리", example = "파티")
                String category,

                @Schema(description = "이미지 URL", example = "https://cdn.mingleup.party/party1.png")
                String partyImageUrl,

                @Schema(description = "모집 상태", example = "recruiting")
                String status
        ) {
                public static PartySummary from(Party party) {
                        return PartySummary.builder()
                                .partyId(party.getId())
                                .title(party.getTitle())
                                .category(party.getCategory())
                                .partyImageUrl(party.getPartyImageUrl())
                                .status(party.getStatus().name().toLowerCase())
                                .build();
                }
        }

        public static PartyListResponse from(int totalCount, int currentPage, List<Party> parties) {
                return PartyListResponse.builder()
                        .totalCount(totalCount)
                        .currentPage(currentPage)
                        .result(parties.stream()
                                .map(PartySummary::from)
                                .collect(Collectors.toList()))
                        .build();
        }
}
