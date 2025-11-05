package com.mingleup.backend.domian.ai.domain;

import com.mingleup.backend.domian.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_group_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ai_group_member_unique",
                        columnNames = {"ai_group_id", "user_id"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AiGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_group_id", nullable = false)
    private AiGroup aiGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public AiGroupMember(AiGroup aiGroup, User user) {
        this.aiGroup = aiGroup;
        this.user = user;
    }
}