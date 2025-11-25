package com.mingleup.backend.global.s3;

import com.mingleup.backend.global.s3.dto.PresignedUrlRequest;
import com.mingleup.backend.global.s3.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request, Long userId) {

        // 폴더 매핑
        String folderPrefix = switch (request.folderType()) {
            case "USER_PROFILE" -> "users/profile/";
            case "PARTY_THUMBNAIL" -> "parties/thumbnail/";
            default -> "etc/";
        };

        // 확장자 추출
        String extension = extractExtension(request.fileName());

        // 실제 S3 객체 key
        String key = folderPrefix + userId + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(
                presigned.url().toString(),
                key
        );
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot == -1) return "";
        return fileName.substring(lastDot);
    }
}
