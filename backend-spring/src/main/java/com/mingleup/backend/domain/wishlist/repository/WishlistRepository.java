package com.mingleup.backend.domain.wishlist.repository;

import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * 특정 사용자가 찜한 모든 내역을 조회합니다.
     * (N+1 문제를 방지하려면 @EntityGraph(attributePaths = {"party"}) 사용 고려)
     * @param user
     * @return
     */
    List<Wishlist> findByUser(User user);

    // (TODO: 찜하기(POST), 찜 취소(DELETE)를 위한 existsByUserAndParty 메서드 추가)
}