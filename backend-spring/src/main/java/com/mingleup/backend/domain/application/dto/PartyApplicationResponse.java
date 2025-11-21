package com.mingleup.backend.domain.application.dto;

import com.mingleup.backend.domain.application.domain.PartyApplication;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 신청 응답 DTO")
public record PartyApplicationResponse(

        @Schema(description = "신청 ID", example = "101")
        Long applicationId,

        @Schema(description = "파티 ID", example = "12")
        Long partyId,

        @Schema(description = "신청자(유저) ID", example = "3")
        Long userId,

        @Schema(description = "신청 상태", example = "PENDING")
        String status,

        @Schema(description = "제출한 답변")
        String answerText

) {
        public static PartyApplicationResponse from(PartyApplication application) {
                return new PartyApplicationResponse(
                        application.getId(),
                        application.getParty().getId(),
                        application.getUser().getId(),
                        application.getStatus().name(),
                        application.getAnswerText()
                );
        }
}
