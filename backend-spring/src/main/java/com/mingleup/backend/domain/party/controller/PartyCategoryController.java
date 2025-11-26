package com.mingleup.backend.domain.party.controller;

import com.mingleup.backend.domain.party.dto.response.PartyCategoryResponse;
import com.mingleup.backend.domain.party.service.PartyCategoryService;
import com.mingleup.backend.global.common.ApiResult;
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

    @GetMapping
    public ApiResult<List<PartyCategoryResponse>> getPartyCategories() {
        List<PartyCategoryResponse> categories = partyCategoryService.getCategories();
        return ApiResult.onSuccess(categories);
    }
}
