package com.mingleup.backend.domain.review.service;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.review.dto.PartyReviewListResponse;
import com.mingleup.backend.domain.review.dto.PartyReviewResponse;
import com.mingleup.backend.domain.review.repository.ReviewRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PartyRepository partyRepository;

    public PartyReviewListResponse getReviewsByParty(Long partyId) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        List<Review> reviews = reviewRepository.findAllByParty(partyId);

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
}
