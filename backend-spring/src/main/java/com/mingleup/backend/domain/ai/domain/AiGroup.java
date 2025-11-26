package com.mingleup.backend.domain.ai.domain;

import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ai_group")
public class AiGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_group_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "matching_reason", length = 500)
    private String matchingReason;

    // --- 연관관계 ---

    // [추가] 그룹에 속한 멤버들 (조회 편의성을 위해 양방향 매핑 추가)
    @OneToMany(mappedBy = "aiGroup", cascade = CascadeType.ALL)
    private List<AiGroupMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "aiGroup", cascade = CascadeType.PERSIST)
    private List<Review> reviews = new ArrayList<>();

    @Builder
    public AiGroup(Party party, String groupName, String matchingReason) {
        this.party = party;
        this.groupName = groupName;
        this.matchingReason = matchingReason;
    }
}