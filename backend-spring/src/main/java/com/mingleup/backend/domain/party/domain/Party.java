package com.mingleup.backend.domain.party.domain;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
import com.mingleup.backend.global.common.BaseTimeEntity;
import com.mingleup.backend.global.common.StringListConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "party")
public class Party extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "guidelines", nullable = false, columnDefinition = "TEXT")
    private String guidelines;

    @Column(name = "party_image_url", nullable = false, length = 2048)
    private String partyImageUrl;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Convert(converter = StringListConverter.class)
    @Column(name = "sub_category")
    private List<String> subCategory = new ArrayList<>();

    @Column(name = "party_datetime", nullable = false)
    private LocalDateTime partyDatetime;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "location_address", nullable = false, length = 500)
    private String locationAddress;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "min_participants", nullable = false)
    private Integer minParticipants;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_method", nullable = false, length = 50)
    private RecruitmentMethod recruitmentMethod;

    @Column(name = "entry_fee", nullable = false)
    private Integer entryFee;

    @Convert(converter = StringListConverter.class)
    @Column(name = "tags")
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PartyStatus status;

    @Column(name = "host_question", columnDefinition = "TEXT")
    private String hostQuestion;

    // --- 연관관계 ---

    // 2. 파티 신청 내역
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyApplication> applications = new ArrayList<>();

    // 3. 파티 찜 내역
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wishlist> wishlists = new ArrayList<>();

    // 4. 파티 후기
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    // 5. 파티의 AI 매칭 그룹
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiGroup> aiGroups = new ArrayList<>();


    @Builder
    public Party(User host, String title, String description, String guidelines, String partyImageUrl, String category, List<String> subCategory, LocalDateTime partyDatetime, String locationName, String locationAddress, Double latitude, Double longitude, Integer minParticipants, Integer maxParticipants, RecruitmentMethod recruitmentMethod, Integer entryFee, List<String> tags, String hostQuestion) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.guidelines = guidelines;
        this.partyImageUrl = partyImageUrl;
        this.category = category;
        this.subCategory = (subCategory != null) ? subCategory : new ArrayList<>();
        this.partyDatetime = partyDatetime;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.minParticipants = (minParticipants != null) ? minParticipants : 2;
        this.maxParticipants = maxParticipants;
        this.recruitmentMethod = recruitmentMethod;
        this.entryFee = (entryFee != null) ? entryFee : 0;
        this.tags = (tags != null) ? tags : new ArrayList<>();
        this.status = PartyStatus.RECRUITING;
        this.hostQuestion = hostQuestion;
    }

    // == 비즈니스 로직 == //
    public void updateStatus(PartyStatus status) {
        this.status = status;
    }

    public void updateThumbnail(String partyImageUrl) { this.partyImageUrl = partyImageUrl; }

    public void updateParty(
            String title,
            String description,
            String guidelines,
            String partyImageUrl,
            String category,
            List<String> subCategory,
            LocalDateTime partyDatetime,
            String locationName,
            String locationAddress,
            Integer minParticipants,
            Integer maxParticipants,
            String recruitmentMethod,
            Integer entryFee,
            List<String> tags,
            String hostQuestion
    ) {
        this.title = title;
        this.description = description;
        this.guidelines = guidelines;
        this.partyImageUrl = partyImageUrl;
        this.category = category;
        this.subCategory = subCategory;
        this.partyDatetime = partyDatetime;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.recruitmentMethod = RecruitmentMethod.valueOf(recruitmentMethod.toUpperCase());
        this.entryFee = entryFee;
        this.tags = tags;
        this.hostQuestion = hostQuestion;
    }
}