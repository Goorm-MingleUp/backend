package com.mingleup.backend.domain.application.domain;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "party_application")
@EntityListeners(AuditingEntityListener.class) // applied_at, updated_at 때문에 BaseTimeEntity 대신 직접 사용
public class PartyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApplicationStatus status;

    @CreatedDate
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- 연관관계 ---

    // 1. 신청서의 답변
    @OneToMany(mappedBy = "partyApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationAnswer> answers = new ArrayList<>();

    @Builder
    public PartyApplication(Party party, User user) {
        this.party = party;
        this.user = user;
        this.status = ApplicationStatus.PENDING; // 생성 시 기본 상태
    }

    // == 비즈니스 로직 == //
    public void updateStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void addAnswer(ApplicationAnswer answer) {
        this.answers.add(answer);
        answer.setPartyApplication(this);
    }
}