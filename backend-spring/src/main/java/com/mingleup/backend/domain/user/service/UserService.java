package com.mingleup.backend.domain.user.service;

import com.mingleup.backend.domain.application.repository.PartyApplicationRepository; // [추가]
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.review.repository.ReviewRepository; // [추가]
import com.mingleup.backend.domain.user.domain.Role; // [추가]
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.dto.*;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // [추가]
import java.math.RoundingMode; // [추가]
import java.util.List; // [추가]
import java.util.stream.Collectors; // [추가]

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PartyApplicationRepository partyApplicationRepository; // [추가]
    private final ReviewRepository reviewRepository; // [추가]



    /**
     * 내 정보 조회 (GET /me)
     * @param userId (토큰에서 추출한)
     * @return
     */
    public UserInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        // GET /me 는 본인 정보이므로 모든 필드를 DTO로 변환
        return UserInfoResponse.from(user);
    }

    /**
     * 내 정보 수정 (PUT /me)
     * @param userId (토큰에서 추출한)
     * @param request (수정할 정보)
     */
    @Transactional
    public void updateMyInfo(Long userId, UpdateUserInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        user.updateUser(
                request.getRegion(),
                request.getMbti(),
                request.getHobbies(),
                request.getIdealTypeHobbies()
        );

        // @Transactional에 의해 변경 감지(dirty checking)로 자동 업데이트됨
    }

    /**
     * 유저 프로필 조회 (GET /{userId})
     * @param targetUserId (조회 대상 ID)
     * @param currentUserId (조회 요청자 ID)
     * @return
     */
    public UserProfileResponse getUserProfile(Long targetUserId, Long currentUserId) {
        // 1. targetUser, currentUser 정보 조회
        User targetUser = findUserById(targetUserId);
        User currentUser = findUserById(currentUserId);

        // 2. 규칙 A: 조회 대상이 '호스트'인 경우
        if (targetUser.getRole() == Role.HOST) {
            // 호스트 프로필은 공개 -> 평점 포함하여 반환
            return UserProfileResponse.from(targetUser, true);
        }

        // 3. 규칙 B: 조회 대상이 '참가자'인 경우
        if (targetUser.getRole() == Role.PARTICIPANT) {
            // 3-1. 예외 1: 본인이 본인 조회
            if (targetUser.getId().equals(currentUserId)) {
                // 평점(hostAvgRating)을 null로 처리하여 반환
                return UserProfileResponse.from(targetUser, false);
            }

            // 3-2. 예외 2: '호스트'가 '신청자' 조회
            if (currentUser.getRole() == Role.HOST) {
                boolean hasApplied = partyApplicationRepository.existsByUserAndParty_Host(targetUser, currentUser);
                if (hasApplied) {
                    // 호스트가 신청자 조회 시 -> 평점 포함하여 반환
                    return UserProfileResponse.from(targetUser, true);
                }
            }

            // 3-3. 그 외 모든 경우 (참가자가 다른 참가자 조회 등)
            throw new CustomException(ErrorCode.FORBIDDEN, "이 프로필을 조회할 권한이 없습니다.");
        }

        // (이론상 도달 불가, Role이 HOST/PARTICIPANT 외에 더 생기지 않는 한)
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "알 수 없는 사용자 역할입니다.");
    }

    /**
     * 유저 후기 목록 조회 (GET /{userId}/reviews)
     * @param targetUserId (조회 대상 ID)
     * @param currentUserId (조회 요청자 ID)
     * @param pageable [수정] 페이징 파라미터 추가
     * @return
     */
    public Page<UserReviewResponse> getUserReviews(Long targetUserId, Long currentUserId, Pageable pageable) { // [수정]
        // 1. targetUser, currentUser 정보 조회
        User targetUser = findUserById(targetUserId);
        User currentUser = findUserById(currentUserId);

        // 2. 규칙 A: 조회 대상이 '호스트'인 경우
        if (targetUser.getRole() == Role.HOST) {
            // 호스트 후기 목록은 공개
            return fetchAndMapReviews(targetUser, pageable); // [수정]
        }

        // 3. 규칙 B: 조회 대상이 '참가자'인 경우
        if (targetUser.getRole() == Role.PARTICIPANT) {
            // 3-1. 예외 1: 본인이 본인 조회
            if (targetUser.getId().equals(currentUserId)) {
                // 명세에 따라 "참가자는 본인이 받은 후기 목록을 조회할 수 없습니다."
                throw new CustomException(ErrorCode.FORBIDDEN, "본인의 후기 목록은 조회할 수 없습니다.");
            }

            // 3-2. 예외 2: '호스트'가 '신청자' 조회
            if (currentUser.getRole() == Role.HOST) {
                boolean hasApplied = partyApplicationRepository.existsByUserAndParty_Host(targetUser, currentUser);
                if (hasApplied) {
                    // 호스트가 신청자 조회 시 -> 허용
                    return fetchAndMapReviews(targetUser, pageable); // [수정]
                }
            }

            // 3-3. 그 외 모든 경우 (참가자가 다른 참가자 조회 등)
            throw new CustomException(ErrorCode.FORBIDDEN, "이 후기 목록을 조회할 권한이 없습니다.");
        }

        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "알 수 없는 사용자 역할입니다.");
    }


    // === Private Helper Methods ===

    /**
     * (Helper) ID로 사용자를 조회하고, 없으면 USER_NOT_FOUND 예외 발생
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다. ID: " + userId));
    }

    /**
     * (Helper) 대상 유저(reviewee)가 받은 후기를 조회하여 DTO 리스트로 변환
     */
    private Page<UserReviewResponse> fetchAndMapReviews(User targetUser, Pageable pageable) { // [수정]
        Page<Review> reviews = reviewRepository.findByReviewee(targetUser, pageable); // [수정]

        // (참고) N+1 문제 최적화가 필요할 수 있음 (Review -> User (reviewer))
        return reviews.map(UserReviewResponse::from); // [수정]
    }

    // --- [추가된 로직] ---

    /**
     * [핵심] 사용자의 평균 평점을 다시 계산하고 업데이트합니다.
     * (ReviewService 등에서 후기 생성/삭제 시 이 메소드를 호출해야 합니다)
     *
     * @param userId 평점을 갱신할 사용자의 ID
     */
    @Transactional // (readOnly=false) CUD 작업을 위해 @Transactional을 붙여줍니다.
    public void updateUserAverageRating(Long userId) {
        User user = findUserById(userId);

        // 1. 해당 유저가 'reviewee'로서 받은 모든 후기를 조회합니다.
        // [수정] findByReviewee(user, pageable)과의 메서드명 중복을 피하기 위해 findAllByReviewee 호출
        List<Review> reviews = reviewRepository.findAllByReviewee(user);

        BigDecimal newRating;

        if (reviews.isEmpty()) {
            newRating = BigDecimal.ZERO;
        } else {
            // 2. 평균 계산 (double)
            double average = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            // 3. BigDecimal로 변환 (소수점 첫째 자리까지, 반올림)
            newRating = BigDecimal.valueOf(average)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        // 4. User 엔티티에 업데이트
        // [중요] User.java 엔티티에 다음 메소드가 반드시 필요합니다:
        //
        // public void updateAvgRating(BigDecimal newRating) {
        //     this.hostAvgRating = newRating;
        // }
        //
        // (참고: UserProfileResponse 에서는 avgRating을 사용하므로
        //  User 엔티티의 필드명(hostAvgRating)과 DTO 필드명(avgRating)이 다름에 유의)
        user.updateAvgRating(newRating); // [수정] user.updateHostAvgRating -> user.updateAvgRating (제공된 코드 기준)

        // @Transactional(readOnly=false)이므로 Dirty Checking에 의해 자동 저장됩니다.
    }

    @Transactional
    public void updateProfileImage(Long userId, UpdateUserProfileImageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfileImage(request.imageUrl());
    }
}