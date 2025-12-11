package org.todayreading.collectingworker.naver.application.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.application.query.policy.NaverPageFetcher;
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
@Slf4j
public class NaverQueryCollector {

  /**
   * 네이버 책 검색 API 포트입니다.
   */
  private final NaverPageFetcher naverPageFetcher;

  /**
   * 네이버 API 검색 관련 설정입니다.
   */
  private final NaverApiProperties naverApiProperties;

  /**
   * 하나의 검색어에 대해 여러 페이지를 호출해 모든 {@link NaverSearchItem}을 수집합니다.
   *
   * @param query    네이버 API 검색어 (null 또는 공백이면 빈 리스트 반환)
   * @param maxStart 최대 start 값 (null이면 설정값 사용)
   * @return 수집된 검색 결과 아이템 전체
   * @since 1.0.0
   */
  public List<NaverSearchItem> collectAllByQuery(String query, Integer maxStart) {
    if (isInvalidQuery(query)) {
      return Collections.emptyList();
    }

    int limitStart = resolveLimitStart(maxStart);
    List<NaverSearchItem> allItems = new ArrayList<>();

    for (int start = 1; start <= limitStart; ) {
      NaverSearchResponse response = naverPageFetcher.fetchPage(query, start);
      // Response가 비어있거나, Items이 없을 경우
      if (isEmptyResponse(response)) {
        break;
      }

      collectPage(response, allItems);

      int nextStart = calculateNextStart(start, response);
      // 다음 페이지로 넘어갈 수 있는지 판단
      if (shouldTerminatePagination(nextStart, limitStart, response)) {
        break;
      }

      start = nextStart;  // 증감식은 여기에 남겨둔다
    }
    return allItems;
  }

  /**
   * 검색어가 null 이거나 공백인지 여부를 반환합니다.
   */
  private boolean isInvalidQuery(String query) {
    return query == null || query.isBlank();
  }

  /**
   * 명시된 maxStart가 있으면 그 값을, 없으면 설정값의 maxStart를 사용합니다.
   */
  private int resolveLimitStart(Integer maxStart) {
    return (maxStart != null)
        ? maxStart
        : naverApiProperties.getSearch().getMaxStart();
  }

  private boolean isEmptyResponse(NaverSearchResponse response) {
    return response == null || hasNoItems(response);
  }

  /**
   * 응답에 아이템이 없는지 여부를 반환합니다.
   */
  private boolean hasNoItems(NaverSearchResponse response) {
    return response.items() == null || response.items().isEmpty();
  }

  private void collectPage(NaverSearchResponse response, List<NaverSearchItem> allItems) {
    allItems.addAll(response.items());
  }

  /**
   * 현재 페이지 정보를 바탕으로 다음 start 값을 계산합니다.
   *
   * <p>display가 0 이하이면 더 이상 진행할 수 없으므로
   * 음수(-1)를 반환해 루프를 종료하도록 신호를 보냅니다.
   */
  private int calculateNextStart(int currentStart, NaverSearchResponse response) {
    int pageSize = response.display();
    if (pageSize <= 0) {
      // 더 이상 정상적인 페이지 진행이 불가능하므로
      // "루프를 종료하라"는 신호로 -1 같은 값을 반환
      return -1;
    }
    return currentStart + pageSize;
  }

  /**
   * 다음 페이지로 페이징을 이어갈 수 없는지 여부를 판단합니다.
   *
   * <p>nextStart가 1 미만이거나, limitStart/total을 초과하면 중단합니다.
   */
  private boolean shouldTerminatePagination(int nextStart, int limitStart,
      NaverSearchResponse response) {
    if (nextStart < 1) {
      return true;
    }
    if (nextStart > limitStart) {
      return true;
    }
    return nextStart > response.total();
  }
}
