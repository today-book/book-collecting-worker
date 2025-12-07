package org.todayreading.collectingworker.naver.application.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.infrastructure.config.NaverApiProperties;

/**
 * 네이버 책 검색 API에서 단일 검색어 기준으로 페이지 단위 수집을 담당합니다.
 *
 * <p>{@link NaverSearchPort}를 통해 외부 API를 호출하고,
 * {@link NaverApiProperties}의 설정(maxStart 등)으로 수집 범위를 제어합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class NaverQueryCollector {

  /** 네이버 책 검색 API 포트입니다. */
  private final NaverSearchPort naverSearchPort;

  /** 네이버 API 검색 관련 설정입니다. */
  private final NaverApiProperties naverApiProperties;

  /**
   * 하나의 검색어에 대해 여러 페이지를 호출해 모든 {@link NaverSearchItem}을 수집합니다.
   *
   * @param query 네이버 API 검색어 (null 또는 공백이면 빈 리스트 반환)
   * @param maxStart 최대 start 값 (null이면 설정값 사용)
   * @return 수집된 검색 결과 아이템 전체
   * @since 1.0.0
   */
  public List<NaverSearchItem> collectAllByQuery(String query, Integer maxStart) {
    if (isInvalidQuery(query)) {
      return Collections.emptyList();
    }

    int limitStart = resolveLimitStart(maxStart);
    int start = 1;
    List<NaverSearchItem> allItems = new ArrayList<>();

    while (start <= limitStart) {
      NaverSearchResponse response = fetchPage(query, start);

      if (hasNoItems(response)) {
        break;
      }

      allItems.addAll(response.items());

      int nextStart = calculateNextStart(start, response);
      if (shouldStop(nextStart, limitStart, response)) {
        break;
      }

      start = nextStart;
    }

    return allItems;
  }

  /** 검색어가 null 이거나 공백인지 여부를 반환합니다. */
  private boolean isInvalidQuery(String query) {
    return query == null || query.isBlank();
  }

  /** 명시된 maxStart가 있으면 그 값을, 없으면 설정값의 maxStart를 사용합니다. */
  private int resolveLimitStart(Integer maxStart) {
    return (maxStart != null)
        ? maxStart
        : naverApiProperties.getSearch().getMaxStart();
  }

  /** 지정된 query와 start로 한 페이지를 조회합니다. */
  private NaverSearchResponse fetchPage(String query, int start) {
    return naverSearchPort.search(query, null, start, null);
  }

  /** 응답에 아이템이 없는지 여부를 반환합니다. */
  private boolean hasNoItems(NaverSearchResponse response) {
    return response.items() == null || response.items().isEmpty();
  }

  /**
   * 현재 페이지 정보를 바탕으로 다음 start 값을 계산합니다.
   *
   * <p>display가 0 이하이면 더 이상 진행하지 않도록 현재 값을 그대로 반환합니다.
   */
  private int calculateNextStart(int currentStart, NaverSearchResponse response) {
    int pageSize = response.display();
    if (pageSize <= 0) {
      return currentStart;
    }
    return currentStart + pageSize;
  }

  /**
   * 다음 호출을 계속할지 여부를 판단합니다.
   *
   * <p>nextStart가 limitStart 또는 total을 초과하면 중단합니다.
   */
  private boolean shouldStop(int nextStart, int limitStart, NaverSearchResponse response) {
    if (nextStart > limitStart) {
      return true;
    }
    return nextStart > response.total();
  }
}
