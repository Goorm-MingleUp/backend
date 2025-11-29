package com.mingleup.backend.domain.application.dto;

import com.mingleup.backend.domain.user.domain.User;
import java.util.List;

public record PartyAttendeesResponse(
        Long partyId,
        int totalCount,
        List<AttendeeUser> users
) {
    public record AttendeeUser(
            Long userId,
            String name,
            String profileImageUrl
    ) {}

    public static PartyAttendeesResponse from(Long partyId, List<User> attendees) {
        List<AttendeeUser> users = attendees.stream()
                .map(u -> new AttendeeUser(
                        u.getId(),
                        u.getName(),
                        u.getProfileImageUrl()
                ))
                .toList();

        return new PartyAttendeesResponse(partyId, attendees.size(), users);
    }
}
