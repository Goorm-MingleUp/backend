package com.mingleup.backend.domain.party.service;

import com.mingleup.backend.domain.party.domain.PartyCategory;
import com.mingleup.backend.domain.party.dto.response.PartyCategoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartyCategoryService {

    public List<PartyCategoryResponse> getCategories() {
        return List.of(PartyCategory.values()).stream()
                .map(cat -> new PartyCategoryResponse(cat.name(), cat.getSubCategories()))
                .toList();
    }
}
