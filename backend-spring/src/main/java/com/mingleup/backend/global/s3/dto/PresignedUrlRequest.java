package com.mingleup.backend.global.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "S3 Presigned URL 요청 DTO")
public record PresignedUrlRequest(

        @Schema(description = "폴더 타입 (USER_PROFILE, PARTY_THUMBNAIL 등)", example = "USER_PROFILE")
        String folderType,

        @Schema(description = "원본 파일명", example = "profile.png")
        String fileName,

        @Schema(description = "콘텐츠 타입", example = "image/png")
        String contentType
) { }
