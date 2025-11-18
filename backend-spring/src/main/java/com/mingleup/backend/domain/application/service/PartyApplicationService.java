package com.mingleup.backend.domain.application.service;

import com.mingleup.backend.domain.application.domain.ApplicationAnswer;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.dto.MyApplicationResponse;
import com.mingleup.backend.domain.application.dto.PartyApplicationCancelResponse;
import com.mingleup.backend.domain.application.dto.PartyApplicationRequest;
import com.mingleup.backend.domain.application.dto.PartyApplicationResponse;
import com.mingleup.backend.domain.application.repository.ApplicationAnswerRepository;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.party.domain.HostQuestion;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import com.mingleup.backend.domain.party.domain.RecruitmentMethod;
import com.mingleup.backend.domain.party.repository.HostQuestionRepository;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyApplicationService {

    private final PartyApplicationRepository partyApplicationRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final HostQuestionRepository hostQuestionRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;

    /**
     * 내 파티 신청 목록 조회
     * @param currentUserId (현재 로그인한 사용자 ID)
     * @param pageable (페이징 정보)
     * @return
     */
    @Transactional(readOnly = true)
    public Page<MyApplicationResponse> getMyApplications(Long currentUserId, Pageable pageable) { // [수정]
        // 1. 사용자 조회
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        // 2. 해당 유저가 신청한 목록(PartyApplication)을 페이징으로 조회
        Page<PartyApplication> applicationsPage = partyApplicationRepository.findByUser(user, pageable); // [수정]

        // 3. DTO로 변환하여 반환
        return applicationsPage.map(MyApplicationResponse::from); // [수정]
    }

    /**
     * 파티 신청
     */
    public PartyApplicationResponse apply(Long partyId, Long userId, PartyApplicationRequest req) {
        if (req == null || req.answer_text() == null || req.answer_text().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (partyApplicationRepository.existsByUserAndParty(user, party)) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        HostQuestion question = hostQuestionRepository.findByParty(party)
                .stream().findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.HOST_QUESTION_NOT_FOUND));

        ApplicationStatus status = (party.getRecruitmentMethod() == RecruitmentMethod.FCFS)
                ? ApplicationStatus.APPROVED : ApplicationStatus.PENDING;

        PartyApplication application = partyApplicationRepository.save(
                PartyApplication.builder()
                        .party(party)
                        .user(user)
                        .build()
        );
        application.updateStatus(status);

        applicationAnswerRepository.save(
                ApplicationAnswer.builder()
                        .partyApplication(application)
                        .hostQuestion(question)
                        .answerText(req.answer_text())
                        .build()
        );

        return new PartyApplicationResponse(
                application.getId(),
                party.getId(),
                user.getId(),
                application.getStatus().name(),
                req.answer_text()
        );
    }

    /**
     * 파티 신청 취소
     */
    public PartyApplicationCancelResponse cancelApplication(Long partyId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        if (party.getStatus() == PartyStatus.COMPLETED) {
            throw new CustomException(ErrorCode.APPLICATION_CANNOT_CANCEL_CLOSED_PARTY);
        }

        PartyApplication application = partyApplicationRepository.findByPartyAndUser(party, user)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        partyApplicationRepository.delete(application);

        return new PartyApplicationCancelResponse(
                party.getId(),
                user.getId(),
                "CANCELLED"
        );
    }
}