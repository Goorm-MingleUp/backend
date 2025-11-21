package com.mingleup.backend.domain.review.service;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.ai.repository.AiGroupRepository;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.review.domain.ReviewType;
import com.mingleup.backend.domain.review.dto.BulkCreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewResponse; // [추가]
import com.mingleup.backend.domain.review.repository.ReviewRepository;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.domain.user.service.UserService;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final UserService userService;
    private final PartyApplicationRepository partyApplicationRepository;
    private final AiGroupRepository aiGroupRepository;

    /**
     * 여러 후기를 일괄 생성하고, 생성된 후기 정보를 반환합니다.
     */
    @Transactional
    public List<CreateReviewResponse> createBulkReviews(Long currentUserId, BulkCreateReviewRequest request) { // [수정] 반환 타입 변경
        List<CreateReviewResponse> responses = new ArrayList<>();

        for (CreateReviewRequest reviewRequest : request.getReviews()) {
            // 내부 로직 실행 후 저장된 엔티티 반환
            Review savedReview = processSingleReview(currentUserId, reviewRequest);
            // 응답 리스트에 추가
            responses.add(CreateReviewResponse.from(savedReview));
        }

        return responses;
    }

    /**
     * 단건 후기 처리 로직 (저장된 Review 엔티티 반환)
     */
    private Review processSingleReview(Long currentUserId, CreateReviewRequest request) { // [수정] 반환 타입 void -> Review
        // 1. 엔티티 조회
        User reviewer = findUserById(currentUserId); // 후기 작성자
        Party party = findPartyById(request.getPartyId());

        // 2. [Validation] 모임 참석 여부 검증
        PartyApplication application = partyApplicationRepository.findByUserAndParty(reviewer, party)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "이 모임에 신청한 내역이 없습니다."));

        if (application.getStatus() != ApplicationStatus.ATTENDED) {
            throw new CustomException(ErrorCode.FORBIDDEN, "이 모임에 '참석 완료' 상태가 아니면 후기를 남길 수 없습니다.");
        }

        // 3. 후기 타입에 따라 대상(reviewee) 및 중복 검증
        ReviewType type = request.getReviewType();
        User reviewee = null;
        AiGroup aiGroup = null;

        switch (type) {
            case HOST:
            case PARTICIPANT:
                if (request.getRevieweeId() == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "HOST 또는 PARTICIPANT 후기에는 'revieweeId'가 필수입니다.");
                }
                reviewee = findUserById(request.getRevieweeId());

                if (reviewer.getId().equals(reviewee.getId())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "자기 자신을 리뷰할 수 없습니다.");
                }

                if (reviewRepository.existsByReviewerAndRevieweeAndParty(reviewer, reviewee, party)) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "해당 모임에서 이 유저에 대한 후기를 이미 작성했습니다. (User ID: " + request.getRevieweeId() + ")");
                }
                break;

            case PARTY:
                if (reviewRepository.existsByReviewerAndPartyAndReviewType(reviewer, party, ReviewType.PARTY)) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "해당 모임에 대한 후기를 이미 작성했습니다.");
                }
                break;

            case AI_GROUP:
                if (request.getAiGroupId() == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "AI_GROUP 후기에는 'aiGroupId'가 필수입니다.");
                }
                aiGroup = findAiGroupById(request.getAiGroupId());

                // [Validation] 이 AI 그룹이 이 파티의 그룹인지 확인
                if (!aiGroup.getParty().getId().equals(party.getId())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "해당 AI 그룹이 모임에 속해있지 않습니다.");
                }

                if (reviewRepository.existsByReviewerAndAiGroup(reviewer, aiGroup)) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "해당 AI 그룹에 대한 후기를 이미 작성했습니다.");
                }
                break;

            default:
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 후기 타입입니다.");
        }


        // 4. Review 엔티티 생성
        Review review = Review.builder()
                .party(party)
                .reviewer(reviewer)
                .reviewType(type)
                .reviewee(reviewee) // HOST/PARTICIPANT가 아니면 null
                .aiGroup(aiGroup) // AI_GROUP이 아니면 null
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        // 5. 후기 저장
        Review savedReview = reviewRepository.save(review); // [수정] 저장된 객체 반환

        // 6. [핵심] 사용자 평점 갱신
        if (reviewee != null) {
            userService.updateUserAverageRating(reviewee.getId());
        }

        return savedReview; // [추가]
    }


    // === Private Helper Methods ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다. ID: " + userId));
    }

    private Party findPartyById(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "모임 정보를 찾을 수 없습니다. ID: " + partyId));
    }

    private AiGroup findAiGroupById(Long aiGroupId) {
        return aiGroupRepository.findById(aiGroupId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 그룹 정보를 찾을 수 없습니다. ID: " + aiGroupId));
    }
}