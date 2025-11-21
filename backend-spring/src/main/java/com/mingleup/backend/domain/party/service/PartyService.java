package com.mingleup.backend.domain.party.service;

import com.mingleup.backend.domain.party.domain.*;
import com.mingleup.backend.domain.party.dto.request.PartyCreateRequest;
import com.mingleup.backend.domain.party.dto.request.PartyUpdateRequest;
import com.mingleup.backend.domain.party.dto.response.HostQuestionResponse;
import com.mingleup.backend.domain.party.dto.response.PartyCreateResponse;
import com.mingleup.backend.domain.party.dto.response.PartyDetailResponse;
import com.mingleup.backend.domain.party.dto.response.PartyListResponse;
import com.mingleup.backend.domain.party.repository.PartyRepository;
import com.mingleup.backend.domain.user.domain.Role;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyService {

    private final PartyRepository partyRepository;
    private final UserRepository userRepository;

    /**
     * 파티 생성
     */
    public PartyCreateResponse createParty(Long userId, PartyCreateRequest req) {

        User host = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (host.getRole() != Role.HOST) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Party party = req.toEntity(host);

        partyRepository.save(party);

        return PartyCreateResponse.from(party);
    }

    /**
     * 파티 상세 조회
     */
    @Transactional(readOnly = true)
    public PartyDetailResponse getParty(Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        return PartyDetailResponse.from(party);
    }

    /**
     * 파티 목록 조회
     */
    @Transactional(readOnly = true)
    public PartyListResponse getParties(
            int page,
            int limit,
            String search,
            String status,
            String category,
            String sortBy
    ) {
       Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
       if ("latest".equalsIgnoreCase(sortBy)) {
           sort = Sort.by(Sort.Direction.DESC, "createdAt");
       } else if ("popular".equalsIgnoreCase(sortBy)) {
           sort = Sort.by(Sort.Direction.DESC, "wishlists.size");
       }

       Pageable pageable = PageRequest.of(page - 1, limit, sort);

       PartyStatus partyStatus = null;
       if (status != null && !status.isBlank()) {
           try {
               partyStatus = PartyStatus.valueOf(status.toUpperCase());
           } catch (IllegalArgumentException e) {
               partyStatus = null;
           }
       }

       Page<Party> partyPage = partyRepository.findAllWithFilters(search, category, partyStatus, pageable);

        return PartyListResponse.from(
                (int) partyPage.getTotalElements(),
                page,
                partyPage.getContent()
       );
    }

    /**
     * 파티 수정
     */
    public void updateParty(Long partyId, Long userId, PartyUpdateRequest req) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.getHost().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        party.updateParty(
                req.title(),
                req.description(),
                req.guidelines(),
                req.party_image_url(),
                req.category(),
                req.sub_category(),
                req.party_datetime(),
                req.location_name(),
                req.location_address(),
                req.min_participants(),
                req.max_participants(),
                req.recruitment_method(),
                req.entry_fee(),
                req.tags(),
                req.host_question()
        );
    }

    /**
     * 호스트 질문 조회
     */
    @Transactional(readOnly = true)
    public HostQuestionResponse getHostQuestion(Long partyId) {

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTY_NOT_FOUND));

        if (party.getHostQuestion() == null || party.getHostQuestion().isBlank()) {
            throw new CustomException(ErrorCode.HOST_QUESTION_NOT_FOUND);
        }

        return new HostQuestionResponse(
                party.getId(),
                party.getHostQuestion()
        );
    }
}
