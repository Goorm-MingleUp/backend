package com.mingleup.backend.domain.party.repository;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.party.domain.PartyStatus;
import com.mingleup.backend.domain.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {

    @Query("""
            SELECT DISTINCT p FROM Party p
            LEFT JOIN FETCH p.wishlists w
            WHERE (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:category IS NULL OR p.category = :category)
            AND (:status IS NULL OR p.status = :status)
            """)
    Page<Party> findAllWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("status") PartyStatus status,
            Pageable pageable
    );

    Long countByHost(User host);
    Long countByHostAndStatus(User host, PartyStatus status);

    Page<Party> findByHost(User host, Pageable pageable);
    Page<Party> findByHostAndStatus(User host, PartyStatus status, Pageable pageable);
}