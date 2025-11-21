package com.mingleup.backend.domain.ai.repository;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiGroupRepository extends JpaRepository<AiGroup, Long> {

    /**
     * [신규] 특정 파티에 생성된 모든 AI 그룹을 조회합니다.
     */
    List<AiGroup> findAllByParty(Party party);
}