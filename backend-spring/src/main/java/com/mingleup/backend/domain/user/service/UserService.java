package com.mingleup.backend.domain.user.service;

import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.dto.MyInfoResponse;
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
     * @param userId (JWT에서 추출한 사용자 ID)
     * @return MyInfoResponse
     */
    public MyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MyInfoResponse.from(user);
    }
}