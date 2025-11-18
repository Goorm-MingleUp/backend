package com.mingleup.backend.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "호스트 질문 응답 DTO (단일 질문 버전)")
public record HostQuestionResponse(

        @Schema(description = "파티 ID", example = "12")
        Long partyId,

        @Schema(description = "호스트 질문 내용", example = "파티에 기대하는 점, 알러지 정보 등을 자유롭게 작성해주세요.")
        String questionText
) {}
