package com.mingleup.backend.domain.ai.repository;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiGroupRepository extends JpaRepository<AiGroup, Long> {
    // (추후 AI 그룹 관련 쿼리 추가)
}