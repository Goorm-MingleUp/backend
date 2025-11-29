package com.mingleup.backend.domain.wishlist.repository;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Page<Wishlist> findByUser(User user, Pageable pageable);

    boolean existsByUserAndParty(User user, Party party);

    Optional<Wishlist> findByUserAndParty(User user, Party party);

    // [추가] 특정 파티의 찜 개수 카운트
    int countByParty(Party party);
}