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

    // --- 연관관계 ---

    // 1. 그룹 멤버
    @OneToMany(mappedBy = "aiGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiGroupMember> members = new ArrayList<>();

    // 2. 그룹에 대한 후기
    @OneToMany(mappedBy = "aiGroup", cascade = CascadeType.PERSIST)
    private List<Review> reviews = new ArrayList<>();

    @Builder
    public AiGroup(Party party, String groupName) {
        this.party = party;
        this.groupName = groupName;
    }
}