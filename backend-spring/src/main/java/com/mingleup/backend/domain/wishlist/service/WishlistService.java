package com.mingleup.backend.domain.wishlist.service;

import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import com.mingleup.backend.domain.wishlist.dto.MyWishlistResponse;
import com.mingleup.backend.domain.wishlist.repository.WishlistRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;

    /**
     * 내 찜 목록 조회
     * @param currentUserId (현재 로그인한 사용자 ID)
     * @return
     */
    public List<MyWishlistResponse> getMyWishlistedParties(Long currentUserId) {
        // 1. 사용자 조회
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        // 2. 해당 유저가 찜한 목록(Wishlist)을 조회
        // (N+1 문제 발생 지점: Wishlist -> Party)
        List<Wishlist> wishlists = wishlistRepository.findByUser(user);

        // 3. DTO로 변환하여 반환
        return wishlists.stream()
                .map(MyWishlistResponse::from)
                .collect(Collectors.toList());
    }
}