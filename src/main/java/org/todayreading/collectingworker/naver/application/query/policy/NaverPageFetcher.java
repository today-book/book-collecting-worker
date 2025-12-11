package org.todayreading.collectingworker.naver.application.query.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverPageFetcher {

  private final NaverSearchPort naverSearchPort;

  private static final int MAX_RETRY_COUNT = 3;
  private static final long BASE_BACKOFF_MS = 1_000L;

  public NaverSearchResponse fetchPage(String query, int start) {
    for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
      try {
        return naverSearchPort.search(query, null, start, null);
      } catch (RestClientResponseException e) {
        int status = e.getRawStatusCode();

        if (status != 429) {
          throw e;
        }

        // log 필요할 시
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

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Thread interrupted during backoff sleep", e);
    }
  }


}
