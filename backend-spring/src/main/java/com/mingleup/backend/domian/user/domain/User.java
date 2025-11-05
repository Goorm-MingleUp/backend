package com.mingleup.backend.domian.user.domain;

import com.mingleup.backend.domian.application.domain.PartyApplication;
import com.mingleup.backend.domian.party.domain.Party;
import com.mingleup.backend.domian.review.domain.Review;
import com.mingleup.backend.domian.wishlist.domain.Wishlist;
import com.mingleup.backend.global.common.BaseTimeEntity;
import com.mingleup.backend.global.common.StringListConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user")
@SQLDelete(sql = "UPDATE user SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ?")
@Where(clause = "deleted_at IS NULL")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "kakao_id", nullable = false, unique = true)
    private String kakaoId;

    @Column(name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column(name = "region")
    private String region;

    @Column(name = "mbti", length = 10)
    private String mbti;

    @Convert(converter = StringListConverter.class)
    @Column(name = "hobbies")
    private List<String> hobbies = new ArrayList<>();

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "host_intro", length = 500)
    private String hostIntro;

    @Column(name = "host_avg_rating", precision = 2, scale = 1, columnDefinition = "DECIMAL(2,1) DEFAULT 0.0")
    private BigDecimal hostAvgRating;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // --- 연관관계 ---

    // 1. 내가 호스팅하는 파티
    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Party> hostedParties = new ArrayList<>();

    // 2. 내가 신청한 내역
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyApplication> applications = new ArrayList<>();

    // 3. 내가 찜한 내역
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wishlist> wishlists = new ArrayList<>();

    // 4. 내가 작성한 후기
    @OneToMany(mappedBy = "reviewer", cascade = CascadeType.PERSIST)
    private List<Review> writtenReviews = new ArrayList<>();

    // 5. 내가 받은 후기
    @OneToMany(mappedBy = "reviewee", cascade = CascadeType.PERSIST)
    private List<Review> receivedReviews = new ArrayList<>();


    @Builder
    public User(String kakaoId, String email, String name, Gender gender, LocalDate birthdate, String region, String mbti, List<String> hobbies, String profileImageUrl, Role role, String hostIntro, BigDecimal hostAvgRating) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.name = name;
        this.gender = gender;
        this.birthdate = birthdate;
        this.region = region;
        this.mbti = mbti;
        this.hobbies = (hobbies != null) ? hobbies : new ArrayList<>();
        this.profileImageUrl = profileImageUrl;
        this.role = (role != null) ? role : Role.PARTICIPANT;
        this.hostIntro = hostIntro;
        this.hostAvgRating = (hostAvgRating != null) ? hostAvgRating : BigDecimal.ZERO;
    }

    // == 비즈니스 로직 == //
    public void updateUser(String name, String region, String mbti, List<String> hobbies) {
        this.name = name;
        this.region = region;
        this.mbti = mbti;
        this.hobbies = hobbies;
    }

    public void updateHostProfile(String hostIntro) {
        this.hostIntro = hostIntro;
    }

    public void updateRole(Role role) {
        this.role = role;
    }
}