package com.mingleup.backend.domain.application.repository;

import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyApplicationRepository extends JpaRepository<PartyApplication, Long> {

    /**
     * 특정 사용자(user)가 특정 호스트(host)가 개최한 파티에 신청한 내역이 있는지 확인합니다.
     * @param user 신청자 (targetUser)
     * @param host 호스트 (currentUser)
     * @return 신청 내역 존재 시 true, 아니면 false
     */
    boolean existsByUserAndParty_Host(User user, User host);
}