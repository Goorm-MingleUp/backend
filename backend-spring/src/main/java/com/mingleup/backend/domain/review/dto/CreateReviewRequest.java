package com.mingleup.backend.domain.review.dto;

import com.mingleup.backend.domain.review.domain.ReviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "모임 ID는 필수입니다.")
    private Long partyId;

    @NotNull(message = "후기 타입은 필수입니다.")
    private ReviewType reviewType; // HOST, PARTICIPANT, PARTY, AI_GROUP

    // (참고) reviewType이 PARTY 또는 AI_GROUP인 경우 revieweeId는 null일 수 있습니다.
    private Long revieweeId; // 후기 대상 유저 ID

    // [수정] AI 그룹 후기를 위한 aiGroupId 필드 추가
    private Long aiGroupId; // 후기 대상 AI 그룹 ID

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하이어야 합니다.")
    private Integer rating;

    private String comment;
}