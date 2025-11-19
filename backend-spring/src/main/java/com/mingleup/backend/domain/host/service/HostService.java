package com.mingleup.backend.domain.host.service;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.host.dto.HostDashboardResponse;
import com.mingleup.backend.domain.host.dto.HostPartyResponse;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostService {

    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final PartyApplicationRepository partyApplicationRepository;

    /**
     * 호스트 대시보드 상단 요약 정보 조회
     */
    public HostDashboardResponse getDashboardSummary(Long userId) {
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long hostedCount = partyRepository.countByHost(host);
        Long pendingCount = partyApplicationRepository.countByParty_HostAndStatus(host, ApplicationStatus.PENDING);
        Long completedCount = partyRepository.countByHostAndStatus(host, PartyStatus.COMPLETED);

        return HostDashboardResponse.of(host, hostedCount, pendingCount, completedCount);
    }

    /**
     * 호스트가 생성한 파티 목록 조회 (필터링 + 페이징)
     */
    public Page<HostPartyResponse> getHostParties(Long userId, PartyStatus status, Pageable pageable) {
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<Party> partyPage;

        if (status != null) {
            partyPage = partyRepository.findByHostAndStatus(host, status, pageable);
        } else {
            partyPage = partyRepository.findByHost(host, pageable);
        }

        return partyPage.map(HostPartyResponse::from);
    }

    /**
     * [신규] 파티 모집 상태 변경 (호스트 전용)
     * 예: RECRUITING -> CLOSED (조기 마감)
     */
    @Transactional
    public void updatePartyStatus(Long userId, Long partyId, PartyStatus newStatus) {
        // 1. 파티 조회
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티 정보를 찾을 수 없습니다. ID: " + partyId));

        // 2. 권한 확인 (본인이 호스트인지)
        if (!party.getHost().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "해당 파티의 상태를 변경할 권한이 없습니다.");
        }

        // 3. 상태 업데이트
        party.updateStatus(newStatus);
    }
}