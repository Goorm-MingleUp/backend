package com.mingleup.backend.domain.wishlist.repository;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserAndParty(User user, Party party);

    Optional<Wishlist> findByUserAndParty(User user, Party party);

    int countByParty(Party party);
}
