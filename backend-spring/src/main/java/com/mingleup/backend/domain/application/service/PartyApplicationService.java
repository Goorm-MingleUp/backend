package com.mingleup.backend.domain.application.service;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.dto.*;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
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

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PartyApplicationService {

    private final PartyApplicationRepository partyApplicationRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        // 중복 신청 체크
        if (partyApplicationRepository.existsByUserAndParty(user, party)) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        PartyApplication application = PartyApplication.builder()
                .party(party)
                .user(user)
                .answerText(req.answer()) // ✨ 단일 답변
                .build();

        partyApplicationRepository.save(application);

        return PartyApplicationResponse.from(application);
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

    @Transactional(readOnly = true)
    public PartyAttendeesResponse getPartyAttendees(Long partyId) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        // 파티 신청 내역에서 ‘승인된’ 참석자 조회
        List<User> attendees = party.getApplications().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.ATTENDED)
                .map(PartyApplication::getUser)
                .sorted(Comparator.comparing(User::getName, Collator.getInstance(Locale.KOREAN))) // 한글 가나다 정렬
                .toList();

        return PartyAttendeesResponse.from(partyId, attendees);
    }

}