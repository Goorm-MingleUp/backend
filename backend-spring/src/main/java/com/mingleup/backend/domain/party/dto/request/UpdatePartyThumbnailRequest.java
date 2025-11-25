package com.mingleup.backend.domain.party.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 썸네일 이미지 업데이트 요청 DTO")
public record UpdatePartyThumbnailRequest(

        @Schema(description = "S3에 업로드된 이미지 URL")
        String imageUrl
) { }