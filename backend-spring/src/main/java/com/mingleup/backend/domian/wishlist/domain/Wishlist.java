package com.mingleup.backend.domian.wishlist.domain;


import com.mingleup.backend.domian.party.domain.Party;
import com.mingleup.backend.domian.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "wishlist",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "wishlist_user_party_unique",
                        columnNames = {"user_id", "party_id"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class) // created_at만 필요
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Wishlist(User user, Party party) {
        this.user = user;
        this.party = party;
    }
}