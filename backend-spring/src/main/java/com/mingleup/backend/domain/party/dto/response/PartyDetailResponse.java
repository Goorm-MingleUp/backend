package com.mingleup.backend.domain.party.dto.response;

import com.mingleup.backend.domain.party.domain.HostQuestion;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import com.mingleup.backend.domain.party.domain.RecruitmentMethod;
import com.mingleup.backend.domain.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "파티 상세 응답 DTO")
@Builder
public record PartyDetailResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "제목", example = "홍대 와인 모임")
        String title,

        @Schema(description = "설명", example = "와인 마실 분 구해요")
        String description,

        @Schema(description = "가이드라인", example = "무단이탈 금지")
        String guidelines,

        @Schema(description = "카테고리", example = "파티")
        String category,

        @Schema(description = "서브 카테고리", example = "[\"와인\"]")
        List<String> subCategory,

        @Schema(description = "파티 일시", example = "2025-11-20T18:00:00")
        LocalDateTime partyDatetime,

        @Schema(description = "위치명", example = "홍대입구역 근처")
        String locationName,

        @Schema(description = "주소", example = "서울 마포구 양화로 155")
        String locationAddress,

        @Schema(description = "위도", example = "37.5563")
        Double latitude,

        @Schema(description = "경도", example = "126.9220")
        Double longitude,

        @Schema(description = "최소 인원", example = "2")
        Integer minParticipants,

        @Schema(description = "최대 인원", example = "6")
        Integer maxParticipants,

        @Schema(description = "모집 방식", example = "approval")
        String recruitmentMethod,

        @Schema(description = "참가비", example = "10000")
        Integer entryFee,

        @Schema(description = "태그", example = "[\"N빵\", \"와인\"]")
        List<String> tags,

        @Schema(description = "상태", example = "recruiting")
        String status,

        @Schema(description = "호스트 정보")
        HostInfo host,

        @Schema(description = "호스트 질문 목록", example = "[\"좋아하는 와인은?\", \"알러지 있나요?\"]")
        List<String> hostQuestions
) {

        @Builder
        public record HostInfo(
                @Schema(description = "호스트 ID", example = "1")
                Long id,
                @Schema(description = "호스트 이름", example = "김민수")
                String name,
                @Schema(description = "프로필 이미지", example = "https://cdn.mingleup.party/user1.png")
                String profileImageUrl,
                @Schema(description = "호스트 소개", example = "와인 애호가입니다.")
                String hostIntro
        ) {}

        public static PartyDetailResponse from(Party party) {
                User host = party.getHost();

                return PartyDetailResponse.builder()
                        .partyId(party.getId())
                        .title(party.getTitle())
                        .description(party.getDescription())
                        .guidelines(party.getGuidelines())
                        .category(party.getCategory())
                        .subCategory(party.getSubCategory())
                        .partyDatetime(party.getPartyDatetime())
                        .locationName(party.getLocationName())
                        .locationAddress(party.getLocationAddress())
                        .latitude(party.getLatitude())
                        .longitude(party.getLongitude())
                        .minParticipants(party.getMinParticipants())
                        .maxParticipants(party.getMaxParticipants())
                        .recruitmentMethod(party.getRecruitmentMethod().name().toLowerCase())
                        .entryFee(party.getEntryFee())
                        .tags(party.getTags())
                        .status(party.getStatus().name().toLowerCase())
                        .host(new HostInfo(
                                host.getId(),
                                host.getName(),
                                host.getProfileImageUrl(),
                                host.getHostIntro()
                        ))
                        .hostQuestions(
                                party.getHostQuestions().stream()
                                        .map(HostQuestion::getQuestionText)
                                        .collect(Collectors.toList())
                        )
                        .build();
        }
}
