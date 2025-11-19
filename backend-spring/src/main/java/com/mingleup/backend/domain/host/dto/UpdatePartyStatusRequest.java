package com.mingleup.backend.domain.host.dto;

import com.mingleup.backend.domain.party.domain.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePartyStatusRequest {

    @Schema(description = "변경할 파티 상태 (RECRUITING: 모집중, CLOSED: 마감, COMPLETED: 종료)", example = "CLOSED")
    @NotNull(message = "변경할 상태값은 필수입니다.")
    private PartyStatus status;
}