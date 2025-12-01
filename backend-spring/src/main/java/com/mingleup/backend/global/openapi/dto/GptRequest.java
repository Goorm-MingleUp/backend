package com.mingleup.backend.global.openai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GptRequest {
    private String model;
    private List<Message> messages;
    private double temperature;

    private ResponseFormat response_format;

    // [추가] 최대 출력 토큰 수 제한 (과금 방지)
    private int max_tokens;

    @Getter
    @Builder
    public static class Message {
        private String role;
        private String content;
    }

    @Getter
    @Builder
    public static class ResponseFormat {
        private String type;
    }
}