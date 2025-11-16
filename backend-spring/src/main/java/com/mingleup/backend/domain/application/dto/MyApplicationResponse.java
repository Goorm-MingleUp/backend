package com.mingleup.backend.domain.application.dto;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.party.domain.Party;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [GET] /api/v1/applications/me 응답 DTO
 */
@Getter
@Builder
public class MyApplicationResponse {

    // PartyApplication 정보
    private Long applicationId;
    private ApplicationStatus applicationStatus;
    private LocalDateTime appliedAt;

    // Party 정보
    private Long partyId;
    private String partyTitle;
    private String partyImageUrl;
    private LocalDateTime partyDatetime;
    private String partyLocationName;

    /**
     * PartyApplication 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     * (N+1 문제 발생 지점)
     */
    public static MyApplicationResponse from(PartyApplication application) {
        Party party = application.getParty(); // Lazy Loading (N+1)

        return MyApplicationResponse.builder()
                .applicationId(application.getId())
                .applicationStatus(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .partyId(party.getId())
                .partyTitle(party.getTitle())
                .partyImageUrl(party.getPartyImageUrl())
                .partyDatetime(party.getPartyDatetime())
                .partyLocationName(party.getLocationName())
                .build();
    }
}