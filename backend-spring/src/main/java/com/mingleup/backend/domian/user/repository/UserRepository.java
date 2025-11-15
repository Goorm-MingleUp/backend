package com.mingleup.backend.domian.user.repository;

import com.mingleup.backend.domian.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 카카오 고유 ID로 사용자를 찾습니다.
     * @param kakaoId
     * @return
     */
    Optional<User> findByKakaoId(String kakaoId);
}