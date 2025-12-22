package org.todayreading.collectingworker.naver.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.pattern.QueryPatternGenerator;
import org.todayreading.collectingworker.naver.application.port.out.NaverBookPublishPort;
import org.todayreading.collectingworker.naver.application.query.NaverQueryCollector;

/**
 * 네이버 도서 전체 수집 후 원시 데이터를 발행하는 배치 전용 애플리케이션 서비스입니다.
 *
 * <p>이 서비스는 전체 풀스캔 배치를 오케스트레이션합니다.
 * {@link #fullScanAndPublish()}를 통해 전체 수집과 발행을 수행합니다.</p>
 *
 * <p>쿼리 패턴 생성은 {@link QueryPatternGenerator},
 * 단일 검색어에 대한 페이징 수집은 {@link NaverQueryCollector},
 * 수집된 결과 발행은 {@link NaverBookPublishPort}에 각각 위임합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverCollectService {

  /** 단일 검색어 기준으로 네이버 API 페이징 수집을 담당하는 컴포넌트입니다. */
  private final NaverQueryCollector naverQueryCollector;

  /** 수집된 원시 도서 데이터를 외부 시스템(Kafka 등)으로 발행하는 포트입니다. */
  private final NaverBookPublishPort bookRawPublishPort;

  /**
   * 초기 또는 가끔 실행하는 전체 풀스캔 배치 유스케이스입니다.
   *
   * <p>{@link QueryPatternGenerator#generateFullScanQueries()}에서 생성한
   * 풀스캔용 쿼리 패턴 목록을 사용하여, 각 쿼리에 대해 전체 페이징 수집을 수행하고
   * 수집된 결과를 Kafka(book.raw 토픽 등)로 발행합니다.</p>
   *
   * <p>{@code maxStart}는 명시하지 않으며,
   * 내부적으로 {@link #scanAndPublish(List, Integer)} 호출 시
   * {@code maxStart == null}로 전달하여 설정값의 max-start를 사용하게 합니다.</p>
   *
   * @author 박성준
   * @since 1.0.0
   */
  public void fullScanAndPublish() {
    List<String> queries = QueryPatternGenerator.generateFullScanQueries();
    scanAndPublish(queries, null);
  }

  /**
   * 공통 스캔/발행 로직을 담당하는 내부 유스케이스입니다.
   *
   * <p>전달된 쿼리 목록을 순회하면서, 각 쿼리에 대해
   * {@link #collectAndPublishByQuery(String, Integer)}를 호출합니다.</p>
   *
   * @param queries  스캔에 사용할 쿼리 목록
   * @param maxStart 최대 start 값 (null이면 설정값의 max-start 사용)
   * @author 박성준
   * @since 1.0.0
   */
  private void scanAndPublish(List<String> queries, Integer maxStart) {
    log.info("Start Naver full scan. queryCount={}, maxStart={}", queries.size(), maxStart);

    for (String query : queries) {
      try {
        collectAndPublishByQuery(query, maxStart);
      } catch (Exception ex) {
        log.warn("Failed to collect/publish for query={} during full scan.", query, ex);
      }
    }

    log.info("Finished Naver full scan.");
  }

  /**
   * 하나의 검색어에 대해 전체 페이징 수집을 수행한 뒤,
   * 수집된 결과를 외부 시스템(Kafka 등)으로 발행하는 내부 유스케이스입니다.
   *
   * <p>검색어가 비어 있거나, 수집 결과가 빈 경우에는 아무 작업도 수행하지 않습니다.</p>
   *
   * @param query    검색어
   * @param maxStart 최대 start 값 (null이면 설정값의 max-start 사용)
   * @author 박성준
   * @since 1.0.0
   */
  private void collectAndPublishByQuery(String query, Integer maxStart) {
    if (isBlankQuery(query)) {
      return;
    }

    List<NaverSearchItem> items = naverQueryCollector.collectAllByQuery(query, maxStart);
    if (items.isEmpty()) {
      return;
    }

    bookRawPublishPort.publish(items);
  }

  /**
   * 검색어가 null 이거나 공백 문자열인지 여부를 판단합니다.
   *
   * @param query 검사할 검색어
   * @return 비어 있으면 {@code true}, 아니면 {@code false}
   * @author 박성준
   * @since 1.0.0
   */
  private boolean isBlankQuery(String query) {
    return query == null || query.isBlank();
  }
}
