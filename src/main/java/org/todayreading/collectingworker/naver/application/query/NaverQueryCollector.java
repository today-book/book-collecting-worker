package org.todayreading.collectingworker.naver.application.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.query.policy.NaverPageFetcher;
import org.todayreading.collectingworker.naver.infrastructure.config.NaverApiProperties;

/**
 * 네이버 책 검색 API에서 단일 검색어 기준으로 페이지 단위 수집을 담당합니다.
 *
 * <p>이 컴포넌트는 {@link NaverPageFetcher}를 통해 네이버 API를 호출하고,
 * {@link NaverApiProperties}의 설정 값(maxStart 등)을 사용해 수집 범위를 제어합니다.
 * 페이징 로직(시작 지점, 종료 조건 판단)과 HTTP 호출/429 처리 로직을 분리하여,
 * 수집 흐름을 보다 명확하게 유지하는 것을 목표로 합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class NaverQueryCollector {

  /**
   * 네이버 책 검색 API 단일 페이지 호출 및
   * 429(Too Many Requests) 재시도 정책을 담당하는 페처입니다.
   */
  private final NaverPageFetcher naverPageFetcher;

  /**
   * 네이버 API 검색 관련 설정(maxStart 등)을 보관하는 설정 객체입니다.
   */
  private final NaverApiProperties naverApiProperties;

  /**
   * 하나의 검색어에 대해 여러 페이지를 호출해 모든 {@link NaverSearchItem}을 수집합니다.
   *
   * <p>검색어가 유효하지 않으면 빈 리스트를 반환하며, 이후에는
   * 설정된 최대 start 값까지 반복적으로 페이지를 조회하면서 아이템을 누적합니다.
   * 네트워크/레이트리밋 이슈는 {@link NaverPageFetcher}에서 처리하며,
   * 응답이 비어 있거나 더 이상 페이징할 수 없는 경우 반복을 종료합니다.</p>
   *
   * @param query    네이버 API 검색어 (null 또는 공백이면 빈 리스트 반환)
   * @param maxStart 최대 start 값 (null이면 설정값의 maxStart 사용)
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

      // Response가 비어있거나, Items이 없을 경우 수집 종료
      if (isEmptyResponse(response)) {
        break;
      }

      collectPage(response, allItems);

      int nextStart = calculateNextStart(start, response);
      // 다음 페이지로 넘어갈 수 있는지 판단
      if (shouldTerminatePagination(nextStart, limitStart, response)) {
        break;
      }

      // 다음 페이지의 start로 진행
      start = nextStart;
    }
    return allItems;
  }

  /**
   * 검색어가 null 이거나 공백인지 여부를 반환합니다.
   *
   * @param query 검사할 검색어
   * @return 검색어가 null 또는 공백이면 {@code true}, 아니면 {@code false}
   */
  private boolean isInvalidQuery(String query) {
    return query == null || query.isBlank();
  }

  /**
   * 명시된 maxStart가 있으면 그 값을, 없으면 설정값의 maxStart를 사용합니다.
   *
   * @param maxStart 호출 시 전달된 최대 start 값 (nullable)
   * @return 실제로 사용할 최대 start 값
   */
  private int resolveLimitStart(Integer maxStart) {
    return (maxStart != null)
        ? maxStart
        : naverApiProperties.getSearch().getMaxStart();
  }

  /**
   * 응답이 비어 있거나 수집할 아이템이 없는 응답인지 여부를 반환합니다.
   *
   * <p>네트워크/레이트리밋 오류로 {@code null} 이 반환된 경우와,
   * 응답 자체는 존재하지만 아이템 리스트가 비어 있는 경우를 모두
   * "더 이상 수집할 데이터가 없음"으로 간주합니다.</p>
   *
   * @param response 네이버 검색 응답
   * @return 응답이 null 이거나 아이템이 없으면 {@code true}, 아니면 {@code false}
   */
  private boolean isEmptyResponse(NaverSearchResponse response) {
    return response == null || hasNoItems(response);
  }

  /**
   * 응답에 아이템이 없는지 여부를 반환합니다.
   *
   * @param response 네이버 검색 응답
   * @return 아이템 리스트가 null 이거나 비어 있으면 {@code true}
   */
  private boolean hasNoItems(NaverSearchResponse response) {
    return response.items() == null || response.items().isEmpty();
  }

  /**
   * 현재 페이지의 아이템을 전체 수집 결과 리스트에 추가합니다.
   *
   * @param response 현재 페이지 응답
   * @param allItems 지금까지 수집된 전체 아이템 리스트
   */
  private void collectPage(NaverSearchResponse response, List<NaverSearchItem> allItems) {
    allItems.addAll(response.items());
  }

  /**
   * 현재 페이지 정보를 바탕으로 다음 start 값을 계산합니다.
   *
   * <p>display가 0 이하이면 더 이상 진행할 수 없으므로
   * 음수(-1)를 반환해 페이징 종료 신호로 사용합니다.</p>
   *
   * @param currentStart 현재 페이지의 start 값
   * @param response     현재 페이지 응답
   * @return 다음 페이지의 start 값, 더 이상 진행 불가 시 음수(-1)
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
   * <p>다음 start 값이 1 미만이거나, 설정된 limitStart 또는
   * 응답의 total 값을 초과하는 경우 더 이상 페이징을 진행하지 않습니다.</p>
   *
   * @param nextStart  다음 페이지의 start 값
   * @param limitStart 설정된 최대 start 값
   * @param response   현재 페이지 응답(전체 total 정보 포함)
   * @return 페이징을 중단해야 하면 {@code true}, 계속 진행 가능하면 {@code false}
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
