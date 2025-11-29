package com.mingleup.backend.domain.user.domain;

import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.review.domain.Review;
import com.mingleup.backend.domain.wishlist.domain.Wishlist;
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
@Table(name = "`user`") // [수정] user가 예약어일 수 있으므로 backtick 추가
@SQLDelete(sql = "UPDATE `user` SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ?")
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

    @Convert(converter = StringListConverter.class)
    @Column(name = "ideal_type_hobbies")
    private List<String> idealTypeHobbies = new ArrayList<>();

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "host_intro", length = 500)
    private String hostIntro;

    @Column(name = "host_Nickname")
    private String hostNickname;

    @Column(name = "host_avg_rating", precision = 2, scale = 1, columnDefinition = "DECIMAL(2,1) DEFAULT 0.0")
    private BigDecimal avgRating;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "kakao_access_token")
    private String kakaoAccessToken;

    // --- 연관관계 (수정 없음) ---
    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Party> hostedParties = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wishlist> wishlists = new ArrayList<>();

    @OneToMany(mappedBy = "reviewer", cascade = CascadeType.PERSIST)
    private List<Review> writtenReviews = new ArrayList<>();

    @OneToMany(mappedBy = "reviewee", cascade = CascadeType.PERSIST)
    private List<Review> receivedReviews = new ArrayList<>();


    @Builder
    public User(String kakaoId, String email, String name, Gender gender, LocalDate birthdate, String region, String mbti, List<String> hobbies, List<String> idealTypeHobbies, String profileImageUrl, Role role, String hostIntro, BigDecimal avgRating) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.name = name;
        this.gender = gender;
        this.birthdate = birthdate;
        this.region = region;
        this.mbti = mbti;
        this.hobbies = (hobbies != null) ? hobbies : new ArrayList<>();
        this.idealTypeHobbies = (idealTypeHobbies != null) ? idealTypeHobbies : new ArrayList<>(); // [추가]
        this.profileImageUrl = profileImageUrl;
        this.role = (role != null) ? role : Role.PARTICIPANT;
        this.hostIntro = hostIntro;
        this.avgRating = (avgRating != null) ? avgRating : BigDecimal.ZERO;
        this.hostNickname = hostNickname;
    }

    // == 비즈니스 로직 == //

    /**
     * 회원 정보 기입 폼(추가 정보)을 기반으로 유저 정보를 업데이트합니다.
     */
    public void updateUser(String region, String mbti, List<String> hobbies, List<String> idealTypeHobbies) {
        this.region = region;
        this.mbti = mbti;
        this.hobbies = (hobbies != null) ? hobbies : new ArrayList<>();
        this.idealTypeHobbies = (idealTypeHobbies != null) ? idealTypeHobbies : new ArrayList<>();
    }

    public void updateHostProfile(String hostIntro, String hostNickname) {
        this.hostIntro = hostIntro;
        this.hostNickname = hostNickname;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateAvgRating(BigDecimal newRating) {
        this.avgRating = newRating;
    }

    public void updateProfileImage(String newProfileImageUrl) { this.profileImageUrl = newProfileImageUrl; }

    public void updateKakaoToken(String accessToken) {
        this.kakaoAccessToken = accessToken;
    }
}