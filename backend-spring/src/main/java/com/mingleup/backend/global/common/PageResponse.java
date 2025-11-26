package com.mingleup.backend.global.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 불필요한 페이징 메타데이터를 제거하고 핵심 정보만 반환하는 래퍼(Wrapper) 클래스
 * - content: 데이터 리스트
 * - pageNumber: 현재 페이지 번호 (0부터 시작)
 * - pageSize: 페이지당 데이터 수
 * - totalElements: 전체 데이터 수
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;

    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }
}