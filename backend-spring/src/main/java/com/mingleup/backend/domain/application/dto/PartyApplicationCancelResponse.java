package com.mingleup.backend.domain.application.dto;

public record PartyApplicationCancelResponse(
        Long party_id,
        Long user_id,
        String status
) {}
