package com.mingleup.backend.domain.review.domain;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
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
@Table(name = "review")
@EntityListeners(AuditingEntityListener.class) // created_at만 필요
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer; // 작성자

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 50)
    private ReviewType reviewType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id")
    private User reviewee; // 대상자 (호스트, 참가자 후기일 때)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_group_id")
    private AiGroup aiGroup; // 대상 그룹 (AI 그룹 후기일 때)

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Review(Party party, User reviewer, ReviewType reviewType, User reviewee, AiGroup aiGroup, Integer rating, String comment) {
        this.party = party;
        this.reviewer = reviewer;
        this.reviewType = reviewType;
        this.reviewee = reviewee;
        this.aiGroup = aiGroup;
        this.rating = rating;
        this.comment = comment;
    }
}