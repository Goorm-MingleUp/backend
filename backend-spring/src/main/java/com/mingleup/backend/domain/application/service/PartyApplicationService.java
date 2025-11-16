package com.mingleup.backend.domain.application.service;

import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.dto.MyApplicationResponse;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyApplicationService {

    private final PartyApplicationRepository partyApplicationRepository;
    private final UserRepository userRepository;

    /**
     * 내 파티 신청 목록 조회
     * @param currentUserId (현재 로그인한 사용자 ID)
     * @return
     */
    public List<MyApplicationResponse> getMyApplications(Long currentUserId) {
        // 1. 사용자 조회
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        // 2. 해당 유저가 신청한 목록(PartyApplication)을 조회
        // (N+1 문제 발생 지점: PartyApplication -> Party)
        List<PartyApplication> applications = partyApplicationRepository.findByUser(user);

        // 3. DTO로 변환하여 반환
        // (참고: N+1 문제를 피하려면 Repository에서 DTO로 바로 조회(projection)하거나
        //  Fetch Join을 사용해야 합니다. 지금은 편의상 Stream.map 사용)
        return applications.stream()
                .map(MyApplicationResponse::from)
                .collect(Collectors.toList());
    }
}