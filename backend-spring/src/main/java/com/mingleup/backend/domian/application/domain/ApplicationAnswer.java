package com.mingleup.backend.domian.application.domain;

import com.mingleup.backend.domian.party.domain.HostQuestion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "application_answer")
public class ApplicationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private PartyApplication partyApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private HostQuestion hostQuestion;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Builder
    public ApplicationAnswer(PartyApplication partyApplication, HostQuestion hostQuestion, String answerText) {
        this.partyApplication = partyApplication;
        this.hostQuestion = hostQuestion;
        this.answerText = answerText;
    }

    // PartyApplication의 addAnswer에서 사용하기 위한 package-private setter
    void setPartyApplication(PartyApplication partyApplication) {
        this.partyApplication = partyApplication;
    }
}