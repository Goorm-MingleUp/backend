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
import java.util.Collections;
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
     * 1단계: AI 매칭 실행 (자동 승인 + 조 편성 + DB 저장)
     */
    @Transactional
    public void runAiMatching(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티를 찾을 수 없습니다."));

        if (!party.getHost().getId().equals(hostId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "매칭을 실행할 권한이 없습니다.");
        }

        // 재매칭 시 기존 그룹 삭제
        List<AiGroup> existingGroups = aiGroupRepository.findAllByParty(party);
        if (!existingGroups.isEmpty()) {
            aiGroupRepository.deleteAll(existingGroups);
            aiGroupRepository.flush();
        }

        // 대기(PENDING)와 승인(APPROVED) 상태인 모든 신청자 조회
        List<PartyApplication> candidates = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Arrays.asList(ApplicationStatus.PENDING, ApplicationStatus.APPROVED)
        );

        if (candidates.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "매칭할 참가자(대기/승인)가 없습니다.");
        }

        log.info("[AI Matching] 파티 '{}' 매칭 실행. 후보 인원: {}명", party.getTitle(), candidates.size());

        // PENDING 상태인 신청자들을 APPROVED로 변경 후 저장
        boolean statusChanged = false;
        for (PartyApplication app : candidates) {
            if (app.getStatus() == ApplicationStatus.PENDING) {
                app.updateStatus(ApplicationStatus.APPROVED);
                statusChanged = true;
            }
        }
        if (statusChanged) {
            partyApplicationRepository.saveAll(candidates);
        }

        performMatchingLogic(party, candidates);
    }

    @Transactional(readOnly = true)
    public List<AiMatchingResponse> getMatchingResults(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        if (!party.getHost().getId().equals(hostId)) throw new CustomException(ErrorCode.FORBIDDEN);

        return aiGroupRepository.findAllByParty(party).stream()
                .map(AiMatchingResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 2단계: 파티 확정 및 알림 일괄 발송
     * - 파티 상태: SCHEDULED
     * - 참가자 상태: APPROVED -> ATTENDED
     * - 알림: 결과 알림(승인/거절) + 매칭 알림(조 편성) 모두 발송
     */
    @Transactional
    public void finalizePartyAndSendNotification(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        if (!party.getHost().getId().equals(hostId)) throw new CustomException(ErrorCode.FORBIDDEN);

        party.updateStatus(PartyStatus.SCHEDULED);
        partyRepository.save(party); // 파티 상태 변경 저장

        sendFinalizationNotifications(party);
    }

    private void performMatchingLogic(Party party, List<PartyApplication> applicants) {
        Collections.shuffle(applicants); // 랜덤 셔플
        int groupSize = 4;
        int groupCount = 0;
        List<User> currentGroupUsers = new ArrayList<>();

        for (int i = 0; i < applicants.size(); i++) {
            currentGroupUsers.add(applicants.get(i).getUser());
            if (currentGroupUsers.size() == groupSize || i == applicants.size() - 1) {
                groupCount++;
                createGroup(party, groupCount, currentGroupUsers);
                currentGroupUsers.clear();
            }
        }
    }

    private void createGroup(Party party, int groupNumber, List<User> users) {
        String groupName = "AI 추천 " + groupNumber + "조";
        String matchingReason = "MBTI 성향과 활동 스타일이 유사한 멤버들입니다.";
        AiGroup aiGroup = AiGroup.builder()
                .party(party).groupName(groupName).matchingReason(matchingReason).build();
        aiGroupRepository.save(aiGroup);

        for (User user : users) {
            aiGroupMemberRepository.save(AiGroupMember.builder().aiGroup(aiGroup).user(user).build());
        }
    }

    /**
     * [수정] 확정 알림 발송 로직
     * - 모든 대상에게 '결과 알림(승인/거절)' 발송
     * - 승인된 사람에게 '매칭 정보 알림' 추가 발송
     */
    private void sendFinalizationNotifications(Party party) {
        List<PartyApplication> applications = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Arrays.asList(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
        );

        List<PartyApplication> updatedApplications = new ArrayList<>();

        for (PartyApplication app : applications) {
            User recipient = app.getUser();
            ApplicationStatus originalStatus = app.getStatus();

            // 1. [공통] 참가 결과 알림 발송 (승인/거절 여부)
            notificationService.sendApplicationResultNotification(recipient, party, originalStatus);

            // 2. [승인자 전용] 상태 변경 및 매칭 정보 알림 발송
            if (originalStatus == ApplicationStatus.APPROVED) {
                Optional<AiGroupMember> memberOpt = aiGroupMemberRepository.findByAiGroup_PartyAndUser(party, recipient);
                if (memberOpt.isPresent()) {
                    AiGroup assignedGroup = memberOpt.get().getAiGroup();

                    // 매칭 정보 알림 (조 편성 결과)
                    notificationService.sendPartyFinalizationNotification(recipient, party, assignedGroup);
                }

                // 상태 변경 (APPROVED -> ATTENDED)
                app.updateStatus(ApplicationStatus.ATTENDED);
                updatedApplications.add(app);
            }
        }

        // 변경된 상태 DB 저장
        if (!updatedApplications.isEmpty()) {
            partyApplicationRepository.saveAll(updatedApplications);
            log.info("참가자 {}명의 상태를 ATTENDED로 변경 완료", updatedApplications.size());
        }
    }
}