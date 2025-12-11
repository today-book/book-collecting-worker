package org.todayreading.collectingworker.naver.application.query.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;

/**
 * 네이버 책 검색 API 단일 페이지 호출과 429(Too Many Requests)에 대한
 * 간단한 재시도/백오프 정책을 담당하는 컴포넌트입니다.
 *
 * <p>이 클래스는 {@link NaverSearchPort}를 사용해 주어진 검색어와 start 값으로
 * 한 페이지를 조회하며, HTTP 429 응답이 발생할 경우 최대 재시도 횟수까지
 * 지수형(1배, 2배, 3배) 대기 시간을 적용한 뒤 다시 호출을 시도합니다.
 * 재시도 후에도 429가 지속되면 null을 반환하여 상위 호출자가 수집을 종료할 수 있도록 합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverPageFetcher {

  /**
   * 네이버 책 검색 API 호출 포트입니다.
   */
  private final NaverSearchPort naverSearchPort;

  /**
   * HTTP 429 응답에 대해 시도할 최대 재시도 횟수입니다.
   */
  private static final int MAX_RETRY_COUNT = 3;

  /**
   * 재시도 시 기본 대기 시간(ms)입니다.
   *
   * <p>실제 대기 시간은 {@code BASE_BACKOFF_MS * attempt}로 계산되어
   * 1초, 2초, 3초와 같이 증가합니다.</p>
   */
  private static final long BASE_BACKOFF_MS = 1_000L;

  /**
   * 지정된 검색어와 시작 위치(start)로 네이버 책 검색 API를 호출합니다.
   *
   * <p>HTTP 429(Too Many Requests)가 발생하는 경우에는 최대
   * {@link #MAX_RETRY_COUNT}회까지 재시도를 수행하며,
   * 재시도 사이에는 {@link #BASE_BACKOFF_MS}를 기준으로 한 대기 시간
   * (1초, 2초, 3초)을 적용합니다. 재시도 후에도 429가 지속되면
   * {@code null}을 반환하여 상위 호출자에서 수집 중단 신호로 사용할 수 있습니다.</p>
   *
   * <p>429 이외의 HTTP 에러 상태 코드에 대해서는 예외를 그대로 전파합니다.</p>
   *
   * @param query 검색에 사용할 쿼리 문자열
   * @param start 네이버 API의 start 파라미터 값
   * @return 정상 응답일 경우 {@link NaverSearchResponse}, 재시도 후에도 429가 지속되면 null
   * @throws RestClientResponseException 429 이외의 HTTP 에러 응답이 발생한 경우
   * @author 박성준
   * @since 1.0.0
   */
  public NaverSearchResponse fetchPage(String query, int start) {
    for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
      try {
        return naverSearchPort.search(query, null, start, null);
      } catch (RestClientResponseException e) {
        int status = e.getRawStatusCode();

        if (status != 429) {
          throw e;
        }

        // 필요 시 상세 로그 활성화
//        log.warn(
//            "Naver API 429 Too Many Requests 발생 - query={}, start={}, attempt={}/{}, status={}, body={}",
//            query,
//            start,
//            attempt,
//            MAX_RETRY_COUNT,
//            status,
//            e.getResponseBodyAsString()
//        );

        if (attempt == MAX_RETRY_COUNT) {
          log.warn(
              "Naver API 429가 {}회 연속 발생하여 수집을 중단합니다. query={}, start={}",
              MAX_RETRY_COUNT,
              query,
              start
          );
          return null;
        }

        sleepQuietly(BASE_BACKOFF_MS * attempt);
      }
    }
    return null;
  }

  /**
   * 지정된 시간 동안 현재 스레드를 대기시킵니다.
   *
   * <p>대기 중 인터럽트가 발생하면 {@link IllegalStateException}을 발생시켜
   * 호출자에게 알립니다.</p>
   *
   * @param millis 대기시킬 시간(ms)
   * @throws IllegalStateException 대기 중 인터럽트가 발생한 경우
   * @author 박성준
   * @since 1.0.0
   */
  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Thread interrupted during backoff sleep", e);
    }
  }
}
