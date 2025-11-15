package com.mingleup.backend.domain.wishlist.service;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import com.mingleup.backend.domain.wishlist.dto.response.WishlistResponse;
import com.mingleup.backend.domain.wishlist.repository.WishlistRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;

    /**
     * 파티 찜 추가
     */
    public WishlistResponse add(Long partyId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        boolean alreadyExists = wishlistRepository.existsByUserAndParty(user, party);
        if (alreadyExists) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        wishlistRepository.save(Wishlist.builder()
                .user(user)
                .party(party)
                .build());

        int wishCount = wishlistRepository.countByParty(party);
        return new WishlistResponse("added", wishCount);
    }

    /**
     * ✅ 파티 찜 취소
     */
    public WishlistResponse remove(Long partyId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        Wishlist wishlist = wishlistRepository.findByUserAndParty(user, party)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        wishlistRepository.delete(wishlist);

        int wishCount = wishlistRepository.countByParty(party);
        return new WishlistResponse("removed", wishCount);
    }
}
