package com.mingleup.backend.domain.user.service;

import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.dto.UserInfoResponse;
import com.mingleup.backend.domain.user.dto.UpdateUserInfoRequest;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 내 정보 조회
     * @param userId (토큰에서 추출한)
     * @return
     */
    public UserInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                // [수정] 예외를 발생시킬 때, 상세 메시지를 함께 전달합니다.
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        return UserInfoResponse.from(user);
    }

    /**
     * 내 정보 수정 (추가 정보 기입)
     * @param userId (토큰에서 추출한)
     * @param request (수정할 정보)
     */
    @Transactional
    public void updateMyInfo(Long userId, UpdateUserInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateUser(
                request.getRegion(),
                request.getMbti(),
                request.getHobbies(),
                request.getIdealTypeHobbies()
        );

        // @Transactional에 의해 변경 감지(dirty checking)로 자동 업데이트됨
    }
}