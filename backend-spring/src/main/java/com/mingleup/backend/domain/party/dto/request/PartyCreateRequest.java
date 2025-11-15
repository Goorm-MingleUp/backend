package com.mingleup.backend.domain.party.dto.request;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.RecruitmentMethod;
import com.mingleup.backend.domain.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "파티 생성 요청 DTO")
public record PartyCreateRequest(

        @Schema(example = "해피 할로윈 인원 구합니다.")
        String title,

        @Schema(example = "같이 즐거운 파티를 해요!")
        String description,

        @Schema(example = "무단이탈 금지")
        String guidelines,

        @Schema(example = "파티")
        String category,

        @Schema(example = "[\"와인\"]")
        List<String> sub_category,

        @Schema(example = "2025-11-20T18:00:00")
        LocalDateTime party_datetime,

        @Schema(example = "동작구")
        String location_name,

        @Schema(example = "서울시 동작구 상도로 123-45, 루프탑")
        String location_address,

        @Schema(example = "37.5665")
        Double latitude,

        @Schema(example = "126.9780")
        Double longitude,

        @Schema(example = "2")
        Integer min_participants,

        @Schema(example = "6")
        Integer max_participants,

        @Schema(example = "approval")
        String recruitment_method,

        @Schema(example = "10000")
        Integer entry_fee,

        @Schema(example = "[\"N빵\"]")
        List<String> tags,

        @Schema(example = "[\"파티 기대하는 점은?\", \"알러지 정보 있나요?\"]")
        List<String> host_questions,

        @Schema(example = "https://cdn.mingleup.party/party1.png")
        String party_image_url
) {
        public Party toEntity(User host) {
                return Party.builder()
                        .host(host)
                        .title(title)
                        .description(description)
                        .guidelines(guidelines)
                        .partyImageUrl(party_image_url)
                        .category(category)
                        .subCategory(sub_category)
                        .partyDatetime(party_datetime)
                        .locationName(location_name)
                        .locationAddress(location_address)
                        .latitude(latitude)
                        .longitude(longitude)
                        .minParticipants(min_participants)
                        .maxParticipants(max_participants)
                        .recruitmentMethod(RecruitmentMethod.valueOf(recruitment_method.toUpperCase()))
                        .entryFee(entry_fee)
                        .tags(tags)
                        .build();
        }
}
