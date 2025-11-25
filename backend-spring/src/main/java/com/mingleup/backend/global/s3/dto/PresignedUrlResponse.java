package com.mingleup.backend.global.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "S3 Presigned URL 응답 DTO")
public record PresignedUrlResponse(

        @Schema(description = "S3 업로드용 Presigned URL")
        String uploadUrl,

        @Schema(description = "S3에 저장될 객체 키 (DB에 저장용)")
        String key
) { }
