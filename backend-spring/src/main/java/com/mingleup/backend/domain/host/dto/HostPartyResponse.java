package com.mingleup.backend.domain.host.dto;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HostPartyResponse {

    private Long partyId;
    private String title;
    private String partyImageUrl;
    private LocalDateTime partyDatetime;
    private String locationName;
    private PartyStatus status;       // 모집중, 마감됨 등
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer entryFee;
    private LocalDateTime createdAt;  // 생성일 (정렬 기준 등)

    public static HostPartyResponse from(Party party) {
        return HostPartyResponse.builder()
                .partyId(party.getId())
                .title(party.getTitle())
                .partyImageUrl(party.getPartyImageUrl())
                .partyDatetime(party.getPartyDatetime())
                .locationName(party.getLocationName())
                .status(party.getStatus())
                .minParticipants(party.getMinParticipants())
                .maxParticipants(party.getMaxParticipants())
                .entryFee(party.getEntryFee())
                .createdAt(party.getCreatedAt())
                .build();
    }
}