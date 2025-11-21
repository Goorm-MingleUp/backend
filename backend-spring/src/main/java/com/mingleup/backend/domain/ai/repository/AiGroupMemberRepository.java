package com.mingleup.backend.domain.ai.repository;

import com.mingleup.backend.domain.ai.domain.AiGroupMember;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // [추가]

@Repository
public interface AiGroupMemberRepository extends JpaRepository<AiGroupMember, Long> {

    List<AiGroupMember> findAllByAiGroup_Party(Party party);

    /**
     * [신규] 특정 파티에서 특정 유저가 속한 그룹 멤버 정보를 조회
     * (유저가 어느 조에 배정되었는지 찾을 때 사용)
     */
    Optional<AiGroupMember> findByAiGroup_PartyAndUser(Party party, User user);
}