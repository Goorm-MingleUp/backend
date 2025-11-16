package com.mingleup.backend.domain.review.repository;

import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 특정 사용자(reviewee)가 받은 후기 목록을 조회합니다.
     * @param reviewee 후기를 받은 대상자 (targetUser)
     * @return 후기 목록
     */
    List<Review> findByReviewee(User reviewee);
}