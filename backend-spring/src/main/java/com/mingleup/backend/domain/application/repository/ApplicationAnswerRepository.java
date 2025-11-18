package com.mingleup.backend.domain.application.repository;

import com.mingleup.backend.domain.application.domain.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
}
