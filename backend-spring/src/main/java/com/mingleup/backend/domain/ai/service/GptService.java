package com.mingleup.backend.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingleup.backend.domain.ai.dto.GptMatchingResult;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import com.mingleup.backend.global.openai.dto.GptRequest;
import com.mingleup.backend.global.openai.dto.GptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public GptMatchingResult getMatchingResult(List<User> users) {
        String usersJson = convertUsersToJsonString(users);

        // [중요] 프롬프트: 사유를 구체적으로 작성하도록 지시
        String systemPrompt = """
                당신은 전문 파티 플래너이자 AI 매칭 전문가입니다.
                주어진 참가자 목록을 바탕으로 최적의 그룹(조)을 편성해야 합니다.
                
                [규칙]
                1. 한 그룹당 인원은 3~5명 사이로 맞춰주세요. (가능하면 4명 권장)
                2. MBTI, 나이(생년월일), 취미, 성별을 종합적으로 고려하여 케미가 잘 맞을 것 같은 사람들끼리 묶어주세요.
                3. 각 그룹별로 '매칭된 구체적인 사유'를 한국어로 작성해주세요. (예: "구성원 모두 E(외향형) 성향이며, '맛집 탐방'이라는 공통 관심사가 있어 즐거운 대화가 예상됩니다.")
                4. 그룹 이름은 창의적이고 재미있게 지어주세요.
                5. 반드시 아래 JSON 형식으로만 응답해주세요.
                
                {
                  "groups": [
                    {
                      "groupName": "그룹 이름",
                      "reason": "매칭 사유",
                      "userIds": [1, 2, 3, 4]
                    }
                  ]
                }
                """;

        List<GptRequest.Message> messages = new ArrayList<>();
        messages.add(GptRequest.Message.builder().role("system").content(systemPrompt).build());
        messages.add(GptRequest.Message.builder().role("user").content("참가자 목록: " + usersJson).build());

        GptRequest request = GptRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.7)
                .response_format(GptRequest.ResponseFormat.builder().type("json_object").build())
                .max_tokens(3000)
                .build();

        try {
            GptResponse response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GptResponse.class)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 매칭 응답이 비어있습니다.");
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            return objectMapper.readValue(content.trim(), GptMatchingResult.class);

        } catch (Exception e) {
            log.error("GPT API 호출 중 에러 발생: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 매칭 서비스 연결 실패");
        }
    }

    private String convertUsersToJsonString(List<User> users) {
        List<Map<String, Object>> userMaps = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("gender", user.getGender());
            map.put("mbti", user.getMbti());
            map.put("hobbies", user.getHobbies());
            // 나이 계산을 위해 생년월일 전달
            map.put("birthdate", user.getBirthdate() != null ? user.getBirthdate().toString() : "정보없음");
            userMaps.add(map);
        }
        try {
            return objectMapper.writeValueAsString(userMaps);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("유저 데이터 변환 실패", e);
        }
    }
}