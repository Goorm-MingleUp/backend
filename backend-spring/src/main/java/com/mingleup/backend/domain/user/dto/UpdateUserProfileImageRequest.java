package com.mingleup.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 프로필 이미지 업데이트 요청 DTO")
public record UpdateUserProfileImageRequest(

        @Schema(description = "S3에 업로드된 이미지 URL", example = "https://s3.ap-northeast-2.amazonaws.com/bucket/users/profile/1/xxx.png")
        String imageUrl
) { }
