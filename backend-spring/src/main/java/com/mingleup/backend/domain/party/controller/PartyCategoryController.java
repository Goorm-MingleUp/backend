package com.mingleup.backend.domain.party.controller;

import com.mingleup.backend.domain.party.dto.response.PartyCategoryResponse;
import com.mingleup.backend.domain.party.service.PartyCategoryService;
import com.mingleup.backend.global.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Party Category", description = "파티 카테고리 조회 API")
@RequestMapping("/api/v1/parties/categories")
public class PartyCategoryController {

    private final PartyCategoryService partyCategoryService;

    @Operation(
            summary = "파티 카테고리 목록 조회",
            description = "모든 파티 카테고리(대분류)와 그에 속한 하위 카테고리(소분류) 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PartyCategoryResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                       "success": true,
                                       "code": "COMMON200",
                                       "message": "성공입니다.",
                                       "result": [
                                         {
                                           "category": "PARTY",
                                           "subCategories": [
                                             "컨셉파티",
                                             "홈파티",
                                             "내향인파티",
                                             "뮤직파티",
                                             "푸드파티",
                                             "와인파티",
                                             "생일파티",
                                             "포틀럭파티"
                                           ]
                                         },
                                         {
                                           "category": "DRINK",
                                           "subCategories": [
                                             "술 약속"
                                           ]
                                         },
                                         {
                                           "category": "STUDY",
                                           "subCategories": [
                                             "스터디",
                                             "코딩",
                                             "독서"
                                           ]
                                         },
                                         {
                                           "category": "GAME",
                                           "subCategories": [
                                             "게임"
                                           ]
                                         },
                                         {
                                           "category": "SPORTS",
                                           "subCategories": [
                                             "러닝",
                                             "클라이밍",
                                             "풋살"
                                           ]
                                         },
                                         {
                                           "category": "TRAVEL",
                                           "subCategories": [
                                             "여행"
                                           ]
                                         },
                                         {
                                           "category": "HOBBY",
                                           "subCategories": [
                                             "취미"
                                           ]
                                         },
                                         {
                                           "category": "FOOD",
                                           "subCategories": [
                                             "식사"
                                           ]
                                         }
                                       ]
                                     }
                            """)
                    )
            )
    })
    @GetMapping
    public ApiResult<List<PartyCategoryResponse>> getPartyCategories() {
        List<PartyCategoryResponse> categories = partyCategoryService.getCategories();
        return ApiResult.onSuccess(categories);
    }
}
