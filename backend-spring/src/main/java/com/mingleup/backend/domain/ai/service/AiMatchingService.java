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

import java.util.*; // [수정] Map 등을 사용하기 위해 util 패키지 전체 import 권장 혹은 구체적 명시
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
     * 1단계: AI 매칭 실행 (조 편성 + DB 저장)
     * - 승인(APPROVED) 상태인 신청자만 대상으로 매칭합니다.
     * - 대기(PENDING) 상태인 신청자는 포함되지 않습니다.
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

        // 승인(APPROVED) 상태인 신청자만 조회
        List<PartyApplication> candidates = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Collections.singletonList(ApplicationStatus.APPROVED)
        );

        if (candidates.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "매칭할 참가자(승인됨)가 없습니다.");
        }

        log.info("[AI Matching] 파티 '{}' 매칭 실행. 후보 인원: {}명", party.getTitle(), candidates.size());

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

        // [추가] 안전장치 1: 최대 인원 제한
        // 한 번에 너무 많은 인원을 보내면 토큰 비용이 급증하고 GPT 응답이 잘릴 수 있습니다.
        if (applicants.size() > 30) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "AI 매칭은 최대 30명까지만 가능합니다.");
        }

        // [추가] 안전장치 2: 최소 인원 제한
        if (applicants.size() < 4) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "AI 매칭을 위해서는 최소 4명의 참가자가 필요합니다.");
        }

        // 신청자들의 User 정보만 추출하여 리스트로 변환 (수정 가능한 리스트)
        List<User> remainingUsers = applicants.stream()
                .map(PartyApplication::getUser)
                .collect(Collectors.toList());

        int groupSize = 4;
        List<List<User>> finalGroups = new ArrayList<>();

        // 1. 우선 4명씩 꽉 채운 그룹들을 만든다 (Greedy Matching)
        while (remainingUsers.size() >= groupSize) {
            User seedUser = remainingUsers.remove(0);
            List<User> currentGroup = new ArrayList<>();
            currentGroup.add(seedUser);

            // 기준 유저와 가장 점수가 높은 순서대로 정렬하여 상위 3명 선발
            remainingUsers.sort((u1, u2) -> {
                int score1 = calculateScore(seedUser, u1);
                int score2 = calculateScore(seedUser, u2);
                return Integer.compare(score2, score1); // 높은 점수가 앞으로
            });

            for (int i = 0; i < groupSize - 1; i++) {
                currentGroup.add(remainingUsers.remove(0));
            }
            finalGroups.add(currentGroup);
        }

        // 2. 남은 인원 처리 (분배)
        if (!remainingUsers.isEmpty()) {
            // 생성된 그룹이 하나도 없는 경우 (총 인원이 4명 미만) -> 남은 인원으로 1개 조 생성
            if (finalGroups.isEmpty()) {
                finalGroups.add(new ArrayList<>(remainingUsers));
            } else {
                // 이미 생성된 조에 한 명씩 분배 (밸런스 고려)
                int groupIndex = 0;
                while (!remainingUsers.isEmpty()) {
                    User user = remainingUsers.remove(0);
                    finalGroups.get(groupIndex).add(user);

                    groupIndex++;
                    if (groupIndex >= finalGroups.size()) {
                        groupIndex = 0; // 다시 첫 번째 그룹부터
                    }
                }
            }
        }

        // 3. 최종 그룹 DB 저장
        int groupNumber = 1;
        for (List<User> groupMembers : finalGroups) {
            createGroup(party, groupNumber++, groupMembers);
        }
    }

    /**
     * [신규] 두 유저 간의 매칭 호감도 점수 계산
     * - 취미 일치: 개당 +10점
     * - MBTI 궁합: +5점 (여기서는 간단히 N/S, T/F 성향 일치 여부로 판단)
     * - 나이 차이: 차이가 클수록 감점 (-1점/년)
     */
    private int calculateScore(User u1, User u2) {
        int score = 0;

        // 1. 취미 일치 점수 (+10점)
        long commonHobbies = u1.getHobbies().stream()
                .filter(h -> u2.getHobbies().contains(h))
                .count();
        score += (int) commonHobbies * 10;

        // 2. MBTI 유사성 점수 (+5점)
        // MBTI가 둘 다 유효한 경우에만 계산 (예: ISTJ의 2번째 글자(N/S)가 같으면 +)
        if (u1.getMbti() != null && u2.getMbti() != null &&
                u1.getMbti().length() == 4 && u2.getMbti().length() == 4) {

            // N/S (직관/감각) 성향이 같으면 대화 코드가 잘 맞음
            if (u1.getMbti().charAt(1) == u2.getMbti().charAt(1)) score += 5;
            // T/F (사고/감정) 성향이 같으면 공감대가 형성됨
            if (u1.getMbti().charAt(2) == u2.getMbti().charAt(2)) score += 5;
        }

        // 3. 나이 차이 페널티 (비슷한 또래 선호 가정)
        if (u1.getBirthdate() != null && u2.getBirthdate() != null) {
            int ageDiff = Math.abs(u1.getBirthdate().getYear() - u2.getBirthdate().getYear());
            // 10살 차이면 -10점
            score -= ageDiff;
        }

        return score;
    }

    private void createGroup(Party party, int groupNumber, List<User> users) {
        String groupName = "AI 추천 " + groupNumber + "조";

        // [수정] 그룹원들의 특성을 분석하여 매칭 사유 자동 생성
        String matchingReason = analyzeMatchingReason(users);

        AiGroup aiGroup = AiGroup.builder()
                .party(party).groupName(groupName).matchingReason(matchingReason).build();
        aiGroupRepository.save(aiGroup);

        for (User user : users) {
            aiGroupMemberRepository.save(AiGroupMember.builder().aiGroup(aiGroup).user(user).build());
        }
    }

    /**
     * [신규] 그룹원들의 공통점(취미, MBTI)을 분석하여 사유 텍스트 생성
     */
    private String analyzeMatchingReason(List<User> users) {
        if (users == null || users.isEmpty()) return "랜덤 매칭된 그룹입니다.";

        StringBuilder reason = new StringBuilder();

        // 1. 공통 취미 분석 (가장 많이 겹치는 취미 1~2개 추출)
        Map<String, Long> hobbyCounts = users.stream()
                .flatMap(u -> u.getHobbies().stream())
                .collect(Collectors.groupingBy(h -> h, Collectors.counting()));

        // 2명 이상이 공유하는 취미 중 상위 2개
        List<String> topHobbies = hobbyCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!topHobbies.isEmpty()) {
            reason.append("'").append(String.join(", ", topHobbies)).append("'");
            reason.append(" 등 공통 관심사와 ");
        } else {
            reason.append("다양한 관심사와 ");
        }

        // 2. MBTI 성향 분석 (E/I 비율로 분위기 파악)
        long eCount = users.stream()
                .filter(u -> u.getMbti() != null && u.getMbti().startsWith("E"))
                .count();

        if (eCount > users.size() / 2) {
            reason.append("활발한(E) 성향을 가진 ");
        } else {
            reason.append("차분한(I) 성향을 가진 ");
        }

        reason.append("멤버들로 구성되었습니다.");

        return reason.toString();
    }

    /**
     * [확정 알림 발송 로직] - 3초 텀 구현
     * 순서: 1. 승인/거절 알림 -> 2. (3초 대기) -> 3. 매칭 결과 알림
     */
    private void sendFinalizationNotifications(Party party) {
        List<PartyApplication> applications = partyApplicationRepository.findAllByPartyAndStatusIn(
                party, Arrays.asList(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
        );

        // 변경된 내용을 저장하기 위해 리스트 별도 관리
        List<PartyApplication> updatedApplications = new ArrayList<>();
        List<User> approvedRecipients = new ArrayList<>();

        // 1. [1차 알림] 참가 결과(승인/거절) 알림 발송
        for (PartyApplication app : applications) {
            User recipient = app.getUser();
            ApplicationStatus originalStatus = app.getStatus();

            notificationService.sendApplicationResultNotification(recipient, party, originalStatus);

            if (app.getStatus() == ApplicationStatus.APPROVED) {
                // 상태 변경: APPROVED -> ATTENDED
                app.updateStatus(ApplicationStatus.ATTENDED);
                updatedApplications.add(app);
                // 2차 알림 대상자로 추가
                approvedRecipients.add(recipient);
            }
        }

        // [상태 업데이트] 변경된 상태를 DB에 강제 저장
        if (!updatedApplications.isEmpty()) {
            partyApplicationRepository.saveAll(updatedApplications);
            log.info("참가자 {}명의 상태를 ATTENDED로 변경 완료", updatedApplications.size());
        }

        // 2. [지연] 3초 대기
        try {
            log.info("알림 발송 간격 조정 (3초 대기)...");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("알림 발송 대기 중 인터럽트 발생");
        }

        // 3. [2차 알림] 매칭 정보(조 편성) 알림 발송 (승인된 참가자만)
        for (User recipient : approvedRecipients) {
            Optional<AiGroupMember> memberOpt = aiGroupMemberRepository.findByAiGroup_PartyAndUser(party, recipient);
            if (memberOpt.isPresent()) {
                AiGroup assignedGroup = memberOpt.get().getAiGroup();

                notificationService.sendPartyFinalizationNotification(recipient, party, assignedGroup);
            }
        }
    }
}