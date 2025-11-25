package com.mingleup.backend.domain.review.repository;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * [수정] 특정 사용자(reviewee)가 받은 후기 목록을 페이징으로 조회합니다.
     * (API 5: GET /api/v1/users/{userId}/reviews)
     * @param reviewee 후기를 받은 대상자 (targetUser)
     * @param pageable
     * @return
     */
    Page<Review> findByReviewee(User reviewee, Pageable pageable); // [수정]

    /**
     * [수정] 특정 사용자(reviewee)가 받은 모든 후기 목록를 조회합니다. (페이징 X)
     * (평점 갱신용)
     * @param reviewee 후기를 받은 대상자 (targetUser)
     * @return 후기 목록
     */
    List<Review> findAllByReviewee(User reviewee); // [수정] findByReviewee -> findAllByReviewee

    /**
     * [신규] 특정 모임(party)에서, 특정 작성자(reviewer)가
     // ... (기존 코드)
     */

    List<Review> findAllByParty(Long partyId);

    boolean existsByReviewerAndRevieweeAndParty(User reviewer, User reviewee, Party party);
}