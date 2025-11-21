package com.mingleup.backend.domain.host.service;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.host.dto.HostDashboardResponse;
import com.mingleup.backend.domain.host.dto.HostPartyApplicationResponse;
import com.mingleup.backend.domain.host.dto.HostPartyResponse;
import com.mingleup.backend.domain.notification.service.NotificationService; // [추가]
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

import java.util.Arrays; // [추가]
import java.util.List; // [추가]

@Service
@RequiredArgsConstructor
public class HostService {

    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final NotificationService notificationService; // [복구] 알림 서비스 다시 주입

    /**
     * 호스트 대시보드 상단 요약 정보 조회
     */
    @Transactional(readOnly = true)
    public HostDashboardResponse getDashboardSummary(Long userId) {
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long hostedCount = partyRepository.countByHost(host);
        Long pendingCount = partyApplicationRepository.countByParty_HostAndStatus(host, ApplicationStatus.PENDING);
        Long completedCount = partyRepository.countByHostAndStatus(host, PartyStatus.COMPLETED);

        return HostDashboardResponse.of(host, hostedCount, pendingCount, completedCount);
    }

    /**
     * 호스트가 생성한 파티 목록 조회
     */
    @Transactional(readOnly = true)
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
     * 파티 모집 상태 변경
     */
    @Transactional
    public void updatePartyStatus(Long userId, Long partyId, PartyStatus newStatus) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티 정보를 찾을 수 없습니다. ID: " + partyId));

        if (!party.getHost().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "해당 파티의 상태를 변경할 권한이 없습니다.");
        }

        party.updateStatus(newStatus);
    }

    /**
     * 파티별 신청자 명단 조회
     */
    @Transactional(readOnly = true)
    public Page<HostPartyApplicationResponse> getPartyApplications(Long userId, Long partyId, ApplicationStatus status, Pageable pageable) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티 정보를 찾을 수 없습니다. ID: " + partyId));

        if (!party.getHost().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "해당 파티의 신청자 목록을 조회할 권한이 없습니다.");
        }

        if (status == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "신청 상태(status) 값은 필수입니다.");
        }

        return partyApplicationRepository.findByPartyAndStatus(party, status, pageable)
                .map(HostPartyApplicationResponse::from);
    }

    /**
     * 참가 신청 승인/거절 (상태만 변경)
     */
    @Transactional
    public void updateApplicationStatus(Long hostUserId, Long applicationId, ApplicationStatus newStatus) {
        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "신청 정보를 찾을 수 없습니다. ID: " + applicationId));

        if (!application.getParty().getHost().getId().equals(hostUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "이 신청을 처리할 권한이 없습니다.");
        }

        application.updateStatus(newStatus);
        // [변경] 여기서는 상태만 변경하고, 알림은 별도 API로 발송합니다.
    }

    /**
     * [신규] 참가 결과 일괄 알림 발송 (버튼 클릭 시 실행)
     * 해당 파티의 '승인' 또는 '거절' 상태인 신청자들에게 알림을 보냅니다.
     */
    @Transactional(readOnly = true)
    public void sendApplicationResultNotifications(Long hostUserId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티를 찾을 수 없습니다."));

        if (!party.getHost().getId().equals(hostUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }

        // 승인(APPROVED) 또는 거절(REJECTED)된 신청자 모두 조회
        List<PartyApplication> targetApplications = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Arrays.asList(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
        );

        if (targetApplications.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "알림을 보낼 대상(승인/거절된 신청자)이 없습니다.");
        }

        // 알림 발송
        for (PartyApplication app : targetApplications) {
            notificationService.sendApplicationResultNotification(app.getUser(), party, app.getStatus());
        }
    }
}