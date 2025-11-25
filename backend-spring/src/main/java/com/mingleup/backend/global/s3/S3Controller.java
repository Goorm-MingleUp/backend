package com.mingleup.backend.global.s3;

import com.mingleup.backend.global.s3.dto.PresignedUrlRequest;
import com.mingleup.backend.global.s3.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @Operation(summary = "S3 이미지 업로드용 Presigned URL 발급")
    @PostMapping("/presigned-url")
    public ResponseEntity<Map<String, Object>> getPresignedUrl(
            @RequestAttribute("userId") Long userId,
            @RequestBody PresignedUrlRequest request
    ) {

        PresignedUrlResponse response = s3Service.generatePresignedUrl(request, userId);

        return ResponseEntity.ok(
                Map.of(
                        "isSuccess", true,
                        "code", "COMMON200",
                        "message", "Presigned URL 발급 성공",
                        "result", response
                )
        );
    }
}
