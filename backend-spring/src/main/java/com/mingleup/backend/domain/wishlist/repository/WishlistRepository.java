package com.mingleup.backend.domain.wishlist.repository;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.stereotype.Repository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserAndParty(User user, Party party);

    Optional<Wishlist> findByUserAndParty(User user, Party party);

    int countByParty(Party party);

    /**
     * [수정] 특정 사용자가 찜한 모든 내역을 페이징으로 조회합니다.
     * (N+1 문제를 방지하려면 @EntityGraph(attributePaths = {"party"}) 사용 고려)
     * @param user
     * @param pageable
     * @return
     */
    Page<Wishlist> findByUser(User user, Pageable pageable); // [수정]

    // (TODO: 찜하기(POST), 찜 취소(DELETE)를 위한 existsByUserAndParty 메서드 추가)
}