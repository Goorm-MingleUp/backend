package com.mingleup.backend.domain.party.domain;

import lombok.Getter;

import java.util.List;

@Getter
public enum PartyCategory {

    PARTY(List.of("컨셉파티", "홈파티", "내향인파티", "뮤직파티", "푸드파티", "와인파티", "생일파티", "포틀럭파티")),
    DRINK(List.of("술 약속")),
    STUDY(List.of("스터디", "코딩", "독서")),
    GAME(List.of("게임")),
    SPORTS(List.of("러닝", "클라이밍", "풋살")),
    TRAVEL(List.of("여행")),
    HOBBY(List.of("취미")),
    FOOD(List.of("식사"));

    private final List<String> subCategories;

    PartyCategory(List<String> subCategories) {
        this.subCategories = subCategories;
    }

}
