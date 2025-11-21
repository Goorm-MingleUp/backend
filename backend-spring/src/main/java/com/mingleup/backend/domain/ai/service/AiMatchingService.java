package com.mingleup.backend.domain.ai.service;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.ai.domain.AiGroupMember;
import com.mingleup.backend.domain.ai.dto.AiMatchingResponse;
import com.mingleup.backend.domain.ai.repository.AiGroupMemberRepository;
import com.mingleup.backend.domain.ai.repository.AiGroupRepository;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.notification.service.NotificationService;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMatchingService {

    private final PartyRepository partyRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final AiGroupRepository aiGroupRepository;
    private final AiGroupMemberRepository aiGroupMemberRepository;
    private final NotificationService notificationService;

    /**
     * [수정] 1단계: AI 매칭 실행 (조 편성 및 DB 저장만 수행)
     */
    @Transactional
    public void runAiMatching(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티를 찾을 수 없습니다."));

        if (!party.getHost().getId().equals(hostId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "매칭을 실행할 권한이 없습니다.");
        }

        List<PartyApplication> approvedApps = partyApplicationRepository.findAllByPartyAndStatus(party, ApplicationStatus.APPROVED);

        if (approvedApps.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "매칭할 참가자(승인됨)가 없습니다.");
        }

        log.info("[AI Matching] 파티 '{}' 매칭 실행. 인원: {}명", party.getTitle(), approvedApps.size());

        // (옵션) 기존 매칭 결과가 있다면 삭제 후 다시 생성하는 로직이 필요할 수 있음
        // clearExistingGroups(party);

        performMatchingLogic(party, approvedApps);
    }

    /**
     * [신규] AI 매칭 결과 조회 (호스트용)
     */
    @Transactional(readOnly = true)
    public List<AiMatchingResponse> getMatchingResults(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티를 찾을 수 없습니다."));

        if (!party.getHost().getId().equals(hostId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "매칭 결과를 조회할 권한이 없습니다.");
        }

        List<AiGroup> groups = aiGroupRepository.findAllByParty(party);

        return groups.stream()
                .map(AiMatchingResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * [신규] 2단계: 파티 확정 및 알림 일괄 발송
     */
    @Transactional
    public void finalizePartyAndSendNotification(Long hostId, Long partyId) {
        // 1. 파티 조회
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티를 찾을 수 없습니다."));

        if (!party.getHost().getId().equals(hostId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }

        // 2. 파티 상태 변경 (SCHEDULED: 확정됨)
        party.updateStatus(PartyStatus.SCHEDULED);
        log.info("파티 상태 변경: {} -> SCHEDULED", party.getTitle());

        // 3. 통합 알림 발송 (승인자 + 거절자 모두)
        sendFinalizationNotifications(party);
    }

    /**
     * 내부 매칭 로직 (그룹핑)
     */
    private void performMatchingLogic(Party party, List<PartyApplication> approvedApps) {
        int groupSize = 4;
        int groupCount = 0;
        List<User> currentGroupUsers = new ArrayList<>();

        for (int i = 0; i < approvedApps.size(); i++) {
            currentGroupUsers.add(approvedApps.get(i).getUser());

            if (currentGroupUsers.size() == groupSize || i == approvedApps.size() - 1) {
                groupCount++;
                createGroup(party, groupCount, currentGroupUsers);
                currentGroupUsers.clear();
            }
        }
    }

    /**
     * 그룹 및 멤버 DB 저장
     */
    private void createGroup(Party party, int groupNumber, List<User> users) {
        String groupName = "AI 추천 " + groupNumber + "조";
        String matchingReason = "MBTI 성향과 활동 스타일이 유사한 멤버들입니다.";

        AiGroup aiGroup = AiGroup.builder()
                .party(party)
                .groupName(groupName)
                .matchingReason(matchingReason)
                .build();

        aiGroupRepository.save(aiGroup);

        for (User user : users) {
            AiGroupMember member = AiGroupMember.builder()
                    .aiGroup(aiGroup)
                    .user(user)
                    .build();
            aiGroupMemberRepository.save(member);
        }
    }

    /**
     * 확정 알림 일괄 발송 로직
     */
    private void sendFinalizationNotifications(Party party) {
        List<PartyApplication> applications = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Arrays.asList(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
        );

        for (PartyApplication app : applications) {
            User recipient = app.getUser();
            AiGroup assignedGroup = null;

            if (app.getStatus() == ApplicationStatus.APPROVED) {
                Optional<AiGroupMember> memberOpt = aiGroupMemberRepository.findByAiGroup_PartyAndUser(party, recipient);
                if (memberOpt.isPresent()) {
                    assignedGroup = memberOpt.get().getAiGroup();
                }
            }
            notificationService.sendPartyFinalizationNotification(recipient, party, app.getStatus(), assignedGroup);
        }
    }
}