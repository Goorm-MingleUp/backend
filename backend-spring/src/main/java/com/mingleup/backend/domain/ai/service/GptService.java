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

    // [추가] 기본값 gpt-4o-mini로 설정 (가성비 모델)
    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.api-url:[https://api.openai.com/v1/chat/completions](https://api.openai.com/v1/chat/completions)}")
    private String apiUrl;

    public GptMatchingResult getMatchingResult(List<User> users) {
        String usersJson = convertUsersToJsonString(users);

        // 시스템 프롬프트: JSON Mode를 쓰려면 반드시 프롬프트에 'JSON'이라는 단어가 포함되어야 함
        String systemPrompt = """
                You are an expert party planner and AI matching specialist.
                Based on the list of participants provided, you need to organize them into optimal groups.
                
                [Rules]
                1. Each group should have 3 to 5 members (ideally 4).
                2. Consider MBTI, age (birthdate), hobbies, and gender to group people with good chemistry.
                3. Provide a specific 'reason' for matching for each group in Korean (e.g., "모두 E 성향이며 활동적인 취미를 공유합니다.").
                4. Create a creative and fun 'groupName' in Korean.
                5. **You must output valid JSON only.** Do not include markdown formatting like ```json.
                
                [Output Format]
                {
                  "groups": [
                    {
                      "groupName": "Group Name",
                      "reason": "Matching Reason",
                      "userIds": [1, 2, 3, 4]
                    }
                  ]
                }
                """;

        List<GptRequest.Message> messages = new ArrayList<>();
        messages.add(GptRequest.Message.builder().role("system").content(systemPrompt).build());
        messages.add(GptRequest.Message.builder().role("user").content("Participant List: " + usersJson).build());

        // [수정] JSON 모드 활성화 요청 객체 생성
        GptRequest request = GptRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.7)
                .response_format(GptRequest.ResponseFormat.builder().type("json_object").build())
                .max_tokens(3000) // [추가] 답변 길이가 3000 토큰을 넘지 못하게 강제 (비용 보호)
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

            // JSON 모드를 썼으므로 마크다운 제거 로직이 거의 필요 없지만, 혹시 모르니 trim 처리
            return objectMapper.readValue(content.trim(), GptMatchingResult.class);

        } catch (Exception e) {
            log.error("GPT API 호출 중 에러 발생: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 매칭 서비스 연결 실패: " + e.getMessage());
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
            map.put("birthdate", user.getBirthdate() != null ? user.getBirthdate().toString() : "Unknown");
            userMaps.add(map);
        }
        try {
            return objectMapper.writeValueAsString(userMaps);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("유저 데이터 변환 실패", e);
        }
    }
}