package com.mingleup.backend.global.openai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GptResponse {
    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    public static class Choice {
        private com.mingleup.backend.global.openai.dto.GptRequest.Message message;
    }
}