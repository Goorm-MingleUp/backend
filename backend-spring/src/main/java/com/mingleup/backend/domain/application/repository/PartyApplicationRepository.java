package com.mingleup.backend.domain.application.repository;

import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.domain.Page; // [추가]
import org.springframework.data.domain.Pageable; // [추가]
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // [추가]
import java.util.Optional;

@Repository
public interface PartyApplicationRepository extends JpaRepository<PartyApplication, Long> {

    /**
     * 특정 사용자(user)가 특정 호스트(host)가 개최한 파티에 신청한 내역이 있는지 확인합니다.
     * @param user 신청자 (targetUser)
     * @param host 호스트 (currentUser)
     * @return 신청 내역 존재 시 true, 아니면 false
     */
    boolean existsByUserAndParty_Host(User user, User host);

    /**
     * [수정] 특정 사용자가 신청한 모든 내역을 페이징으로 조회합니다.
     * (N+1 문제를 방지하려면 @EntityGraph(attributePaths = {"party"}) 사용 고려)
     * @param user
     * @param pageable
     * @return
     */
    Page<PartyApplication> findByUser(User user, Pageable pageable); // [수정]

    boolean existsByUserAndParty(User user, Party party);

    Optional<PartyApplication> findByPartyAndUser(Party party, User user);

    Optional<PartyApplication> findByUserAndParty(User user, Party party);
}