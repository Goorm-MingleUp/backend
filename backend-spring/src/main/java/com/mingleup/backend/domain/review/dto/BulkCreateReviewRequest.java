package com.mingleup.backend.domain.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkCreateReviewRequest {

    @Valid // 내부 리스트 요소들의 Validation도 수행
    @NotNull(message = "후기 목록은 필수입니다.")
    @Size(min = 1, message = "최소 1개 이상의 후기가 필요합니다.")
    private List<CreateReviewRequest> reviews;
}