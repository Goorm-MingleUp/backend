package com.mingleup.backend.domain.party.repository;

import com.mingleup.backend.domain.party.domain.HostQuestion;
import com.mingleup.backend.domain.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostQuestionRepository extends JpaRepository<HostQuestion, Long> {
    List<HostQuestion> findByParty(Party party);
}