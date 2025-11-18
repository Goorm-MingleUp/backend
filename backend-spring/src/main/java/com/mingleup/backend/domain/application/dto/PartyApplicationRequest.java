package com.mingleup.backend.domain.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 신청 요청 DTO")
public record PartyApplicationRequest(
        @Schema(description = "호스트 질문에 대한 답변", example = "분위기 좋게 함께 즐기고 싶어요. 알러지 없어요!")
        String answer_text
) {}
