package com.mingleup.backend.domain.party.repository;

import com.mingleup.backend.domain.party.domain.HostQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostQuestionRepository extends JpaRepository<HostQuestion, Long> {
}