package com.mingleup.backend.domain.user.repository;

import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}