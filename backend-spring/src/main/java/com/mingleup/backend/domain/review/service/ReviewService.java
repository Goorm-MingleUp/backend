package com.mingleup.backend.domain.review.service;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.review.dto.PartyReviewListResponse;
import com.mingleup.backend.domain.review.dto.PartyReviewResponse;
import com.mingleup.backend.domain.review.repository.ReviewRepository;
import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.ai.repository.AiGroupRepository;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.application.repository.PartyApplicationRepository;
import com.mingleup.backend.domain.review.domain.ReviewType;
import com.mingleup.backend.domain.review.dto.BulkCreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewRequest;
import com.mingleup.backend.domain.review.dto.CreateReviewResponse; // [추가]
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

    @Transactional
    public List<CreateReviewResponse> createBulkReviews(Long currentUserId, BulkCreateReviewRequest request) {
        List<CreateReviewResponse> responses = new ArrayList<>();
        for (CreateReviewRequest reviewRequest : request.getReviews()) {
            Review savedReview = processSingleReview(currentUserId, reviewRequest);
            responses.add(CreateReviewResponse.from(savedReview));
        }
        return responses;
    }

    /**
     * 단건 후기 처리 로직 (생성 또는 수정)
     */
    private Review processSingleReview(Long currentUserId, CreateReviewRequest request) {
        User reviewer = findUserById(currentUserId);
        Party party = findPartyById(request.getPartyId());

        // 1. 참석 여부 검증
        PartyApplication application = partyApplicationRepository.findByUserAndParty(reviewer, party)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "이 모임에 신청한 내역이 없습니다."));

        if (application.getStatus() != ApplicationStatus.ATTENDED) {
            throw new CustomException(ErrorCode.FORBIDDEN, "이 모임에 '참석 완료' 상태가 아니면 후기를 남길 수 없습니다.");
        }

        ReviewType type = request.getReviewType();
        User reviewee = null;
        AiGroup aiGroup = null;
        Review existingReview = null;

        // 2. 타입별 대상 조회 및 기존 후기 검색
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
                // [수정] 기존 후기가 있는지 조회 (Upsert)
                existingReview = reviewRepository.findByReviewerAndRevieweeAndParty(reviewer, reviewee, party).orElse(null);
                break;

            case PARTY:
                // [수정] 기존 후기가 있는지 조회 (Upsert)
                existingReview = reviewRepository.findByReviewerAndPartyAndReviewType(reviewer, party, ReviewType.PARTY).orElse(null);
                break;

            case AI_GROUP:
                if (request.getAiGroupId() == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "AI_GROUP 후기에는 'aiGroupId'가 필수입니다.");
                }
                aiGroup = findAiGroupById(request.getAiGroupId());
                if (!aiGroup.getParty().getId().equals(party.getId())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "해당 AI 그룹이 모임에 속해있지 않습니다.");
                }
                // [수정] 기존 후기가 있는지 조회 (Upsert)
                existingReview = reviewRepository.findByReviewerAndAiGroup(reviewer, aiGroup).orElse(null);
                break;

            default:
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 후기 타입입니다.");
        }

        Review reviewToSave;

        // 3. 생성 또는 수정 처리
        if (existingReview != null) {
            // [수정] 기존 후기가 있으면 내용 업데이트
            existingReview.update(request.getRating(), request.getComment());
            reviewToSave = existingReview;
        } else {
            // [신규] 없으면 새로 생성
            reviewToSave = Review.builder()
                    .party(party)
                    .reviewer(reviewer)
                    .reviewType(type)
                    .reviewee(reviewee)
                    .aiGroup(aiGroup)
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .build();
        }

        // 4. 저장 (수정의 경우 Dirty Checking이 동작하지만 명시적으로 save 호출)
        Review savedReview = reviewRepository.save(reviewToSave);

        // 5. 평점 갱신 (수정된 평점 반영을 위해 반드시 호출)
        if (reviewee != null) {
            userService.updateUserAverageRating(reviewee.getId());
        }

        return savedReview;
    }

    public PartyReviewListResponse getReviewsByParty(Long partyId) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        List<Review> reviews = reviewRepository.findAllByPartyId(partyId);

        List<PartyReviewResponse> reviewResponses = reviews.stream()
                .map(review -> new PartyReviewResponse(
                        review.getId(),
                        review.getReviewer().getId(),
                        review.getReviewer().getHostNickname(),
                        review.getReviewer().getProfileImageUrl(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()
                ))
                .toList();

        return new PartyReviewListResponse(partyId, reviewResponses);
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
