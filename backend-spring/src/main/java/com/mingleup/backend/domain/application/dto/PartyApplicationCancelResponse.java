package com.mingleup.backend.domain.application.dto;

public record PartyApplicationCancelResponse(
        Long partyId,
        Long userId,
        String status
) {}
