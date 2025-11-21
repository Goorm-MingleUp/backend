package com.mingleup.backend.domain.host.dto;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateApplicationStatusRequest {

    @Schema(description = "변경할 신청 상태 (APPROVED: 승인, REJECTED: 거절)", example = "APPROVED")
    @NotNull(message = "변경할 상태값은 필수입니다.")
    private ApplicationStatus status;
}