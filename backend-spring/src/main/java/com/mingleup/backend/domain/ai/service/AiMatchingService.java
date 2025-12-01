package com.mingleup.backend.domain.ai.service;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.ai.domain.AiGroupMember;
import com.mingleup.backend.domain.ai.dto.AiMatchingResponse;
import com.mingleup.backend.domain.ai.dto.GptMatchingResult; // [필수] GPT 결과 DTO
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

import java.util.*;
import java.util.function.Function;
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
    private final GptService gptService; // [핵심] GPT 서비스 주입

    /**
     * 1단계: AI 매칭 실행
     * - 실제 GPT API를 호출하여 그룹핑과 매칭 사유를 생성합니다.
     */
    @Transactional
    public void runAiMatching(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파티를 찾을 수 없습니다."));

        if (!party.getHost().getId().equals(hostId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "매칭을 실행할 권한이 없습니다.");
        }

        // 1. 기존 매칭 결과 삭제 (재매칭 허용)
        List<AiGroup> existingGroups = aiGroupRepository.findAllByParty(party);
        if (!existingGroups.isEmpty()) {
            aiGroupRepository.deleteAll(existingGroups);
            aiGroupRepository.flush();
        }

        // 2. 승인(APPROVED)된 참가자 조회
        // (참고: PENDING 상태 자동 승인 로직은 제거됨. 호스트가 승인한 사람만 매칭)
        List<PartyApplication> candidates = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Collections.singletonList(ApplicationStatus.APPROVED)
        );

        if (candidates.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "매칭할 참가자(승인됨)가 없습니다.");
        }

        // 최소 인원 체크 (GPT 비용 절약 및 품질 보장)
        if (candidates.size() < 2) { // 최소 2명 이상이어야 매칭 가능
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "매칭을 위해서는 최소 2명의 참가자가 필요합니다.");
        }

        log.info("[AI Matching] GPT 매칭 시작. 대상: {}명", candidates.size());

        // 3. GPT 매칭 로직 실행
        performGptMatching(party, candidates);
    }

    /**
     * [핵심] GPT API를 이용한 지능형 매칭 로직
     */
    private void performGptMatching(Party party, List<PartyApplication> applicants) {
        // User 리스트 추출
        List<User> users = applicants.stream()
                .map(PartyApplication::getUser)
                .collect(Collectors.toList());

        // User ID로 객체를 빠르게 찾기 위한 Map 생성
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // GPT API 호출
        GptMatchingResult result;
        try {
            result = gptService.getMatchingResult(users);
        } catch (Exception e) {
            log.error("GPT 매칭 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 매칭 중 오류가 발생했습니다: " + e.getMessage());
        }

        log.info("GPT 매칭 완료. {}개 그룹 생성됨.", result.getGroups().size());

        // 결과 저장
        for (GptMatchingResult.GroupResult groupResult : result.getGroups()) {
            // 1. 그룹 생성 (GPT가 지어준 이름과 사유 저장)
            AiGroup aiGroup = AiGroup.builder()
                    .party(party)
                    .groupName(groupResult.getGroupName())
                    .matchingReason(groupResult.getReason())
                    .build();
            aiGroupRepository.save(aiGroup);

            // 2. 멤버 저장
            for (Long userId : groupResult.getUserIds()) {
                User user = userMap.get(userId);
                if (user != null) {
                    aiGroupMemberRepository.save(AiGroupMember.builder()
                            .aiGroup(aiGroup)
                            .user(user)
                            .build());
                }
            }
        }
    }

    // ... (getMatchingResults, finalizePartyAndSendNotification 등 기존 조회/확정 로직 유지) ...

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
     */
    @Transactional
    public void finalizePartyAndSendNotification(Long hostId, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        if (!party.getHost().getId().equals(hostId)) throw new CustomException(ErrorCode.FORBIDDEN);

        party.updateStatus(PartyStatus.SCHEDULED);
        partyRepository.save(party);

        sendFinalizationNotifications(party);
    }

    private void sendFinalizationNotifications(Party party) {
        List<PartyApplication> applications = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Arrays.asList(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
        );

        List<PartyApplication> updatedApplications = new ArrayList<>();
        List<User> approvedRecipients = new ArrayList<>();

        for (PartyApplication app : applications) {
            User recipient = app.getUser();
            ApplicationStatus originalStatus = app.getStatus();

            // 1. 결과 알림 (승인/거절)
            notificationService.sendApplicationResultNotification(recipient, party, originalStatus);

            if (app.getStatus() == ApplicationStatus.APPROVED) {
                app.updateStatus(ApplicationStatus.ATTENDED);
                updatedApplications.add(app);
                approvedRecipients.add(recipient);
            }
        }

        if (!updatedApplications.isEmpty()) {
            partyApplicationRepository.saveAll(updatedApplications);
        }

        // 3초 대기 (알림 순서 보장용)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2. 매칭 정보 알림 (승인자만)
        for (User recipient : approvedRecipients) {
            Optional<AiGroupMember> memberOpt = aiGroupMemberRepository.findByAiGroup_PartyAndUser(party, recipient);
            if (memberOpt.isPresent()) {
                AiGroup assignedGroup = memberOpt.get().getAiGroup();
                notificationService.sendPartyFinalizationNotification(recipient, party, assignedGroup);
            }
        }
    }
}