package com.mingleup.backend.domain.host.dto;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkUpdateApplicationStatusRequest {

    @Schema(description = "변경할 신청 내역 ID 목록", example = "[10, 11, 12]")
    @NotNull(message = "신청 ID 목록은 필수입니다.")
    @Size(min = 1, message = "최소 1개 이상의 신청 ID가 필요합니다.")
    private List<Long> applicationIds;

    @Schema(description = "변경할 신청 상태 (APPROVED, REJECTED)", example = "APPROVED")
    @NotNull(message = "변경할 상태값은 필수입니다.")
    private ApplicationStatus status;
}