package org.todayreading.collectingworker.naver.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Naver 책 검색 API의 최상위 응답을 표현하는 DTO이다.
 *
 * <p>{@code lastBuildDate}, {@code total}, {@code start}, {@code display}와
 * 개별 도서 정보를 담은 {@link NaverSearchItem} 목록을 포함한다.</p>
 *
 * <p>Naver API 응답 JSON을 그대로 매핑하는 용도로 사용하며,
 * 별도의 비즈니스 로직은 포함하지 않는다.</p>
 *
 * author 박성준
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverSearchResponse(
    String lastBuildDate,
    int total,
    int start,
    int display,
    List<NaverSearchItem> items
) {
}
