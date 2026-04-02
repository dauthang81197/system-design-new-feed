package com.pv.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Generic paginated response — dùng chung cho mọi service.
 *
 * @param <T> kiểu dữ liệu trong content
 */
@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}

