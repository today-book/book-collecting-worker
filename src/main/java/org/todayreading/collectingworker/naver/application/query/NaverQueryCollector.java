package org.todayreading.collectingworker.naver.application.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.infrastructure.config.NaverApiProperties;
import org.todayreading.collectingworker.naver.infrastructure.config.NaverApiProperties.SearchProperties;

/**
 * 네이버 책 검색 API에서 단일 검색어 기준으로 페이지 단위 수집을 담당합니다.
 *
 * <p>{@link NaverSearchPort}를 통해 외부 API를 호출하고,
 * {@link NaverApiProperties}의 설정(maxStart 등)으로 수집 범위를 제어합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverQueryCollector {

  private static final int MAX_429_RETRIES = 3;
  private static final int SLOW_MODE_THRESHOLD = 3; // 연속 429 발생 횟수
  private static final long SLOW_MODE_DURATION_MS = 180_000; // 슬로모드 유지 시간(3분)
  private static final int SLOW_MODE_MULTIPLIER = 2; // 슬로모드 시 지연 배수

  /** 네이버 책 검색 API 포트입니다. */
  private final NaverSearchPort naverSearchPort;

  /** 네이버 API 검색 관련 설정입니다. */
  private final NaverApiProperties naverApiProperties;

  /** 연속된 429 발생 횟수입니다. */
  private int consecutive429 = 0;

  /** 슬로모드가 해제될 시각입니다. */
  private Instant slowModeUntil = Instant.EPOCH;

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
      NaverSearchResponse response = fetchPageWithRetry(query, start);

      if (hasNoItems(response)) {
        break;
      }

      allItems.addAll(response.items());

      int nextStart = calculateNextStart(start, response);
      if (shouldStop(nextStart, limitStart, response)) {
        break;
      }

      start = nextStart;
      sleepWithJitter(resolveRequestInterval());
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

  /** 설정에 지정된 요청 간 최소 지연(ms)을 반환합니다. */
  private long resolveRequestInterval() {
    SearchProperties search = naverApiProperties.getSearch();
    return search.getRequestIntervalMs();
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

  private void sleepWithJitter(long baseMillis) {
    if (baseMillis <= 0) {
      return;
    }
    long jitterRange = Math.max(1, baseMillis / 6); // base 300ms → 최대 +50ms
    long jitter = ThreadLocalRandom.current().nextLong(0, jitterRange + 1);
    long multiplier = isSlowMode() ? SLOW_MODE_MULTIPLIER : 1;
    long sleepMillis = (baseMillis + jitter) * multiplier;
    try {
      Thread.sleep(sleepMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * 429(Too Many Requests) 발생 시 기하급수 백오프와 슬로모드를 적용하며 재시도합니다.
   */
  private NaverSearchResponse fetchPageWithRetry(String query, int start) {
    int attempt = 0;
    while (true) {
      try {
        NaverSearchResponse response = fetchPage(query, start);
        consecutive429 = 0; // 성공 시 리셋
        return response;
      } catch (Exception ex) {
        if (!isTooManyRequests(ex)) {
          throw ex;
        }

        consecutive429++;
        attempt++;
        long backoffSeconds = exponentialBackoffSeconds(attempt);
        log.warn(
            "Naver API rate limit (429). query='{}' start={} attempt={} backoff={}s consecutive429={}",
            query,
            start,
            attempt,
            backoffSeconds,
            consecutive429
        );

        if (consecutive429 >= SLOW_MODE_THRESHOLD) {
          slowModeUntil = Instant.now().plusMillis(SLOW_MODE_DURATION_MS);
          log.warn(
              "Entering slow mode for {}ms (multiplier x{}).",
              SLOW_MODE_DURATION_MS,
              SLOW_MODE_MULTIPLIER
          );
        }

        sleepSeconds(backoffSeconds);

        if (attempt >= MAX_429_RETRIES) {
          throw ex;
        }
      }
    }
  }

  private long exponentialBackoffSeconds(int attempt) {
    // 1회차 2초, 2회차 4초, 3회차 8초
    long base = 2L;
    return base << (attempt - 1);
  }

  private boolean isTooManyRequests(Exception ex) {
    return ex.getMessage() != null && ex.getMessage().contains("429");
  }

  private boolean isSlowMode() {
    return Instant.now().isBefore(slowModeUntil);
  }

  private void sleepSeconds(long seconds) {
    if (seconds <= 0) {
      return;
    }
    try {
      Thread.sleep(seconds * 1000L);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
