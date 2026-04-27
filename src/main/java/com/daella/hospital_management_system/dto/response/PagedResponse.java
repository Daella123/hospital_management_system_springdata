package com.daella.hospital_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * <p>Converts a Spring {@link Page} into a flat JSON shape that clearly exposes
 * pagination metadata alongside the content, as required by the Spring Data JPA lab:
 *
 * <pre>
 * {
 *   "content":       [...],
 *   "pageNumber":    0,
 *   "pageSize":      10,
 *   "totalElements": 42,
 *   "totalPages":    5,
 *   "last":          false
 * }
 * </pre>
 *
 * @param <T> the type of elements in {@code content}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int     pageNumber;
    private int     pageSize;
    private long    totalElements;
    private int     totalPages;
    private boolean last;

    /**
     * Convenience factory — builds a {@link PagedResponse} directly from a Spring {@link Page}.
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
