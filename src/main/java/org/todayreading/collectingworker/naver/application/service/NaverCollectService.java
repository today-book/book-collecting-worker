package org.todayreading.collectingworker.naver.application.service;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.BookRawPublishPort;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.application.query.NaverQueryCollector;

/**
 * 네이버 책 검색/수집 및 발행 유스케이스를 담당하는 애플리케이션 서비스입니다.
 *
 * <p>이 서비스는 다음과 같은 흐름을 오케스트레이션합니다:
 * <ul>
 *   <li>단일 페이지 검색:
 *   {@link #searchSinglePage(String, Integer, Integer, String)}</li>
 *   <li>쿼리 1개에 대한 전체 페이징 수집:
 *   {@link #collectAllByQuery(String, Integer)}</li>
 *   <li>쿼리 1개에 대한 수집 후 외부 시스템(Kafka/RabbitMQ 등)으로 발행:
 *   {@link #collectAndPublishByQuery(String, Integer)}</li>
 * </ul>
 *
 * <p>실제 페이징 수집 로직은 {@link NaverQueryCollector}에 위임되며,
 * 외부 API 호출은 {@link NaverSearchPort},
 * 메시지 발행은 {@link BookRawPublishPort}를 통해 수행됩니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class NaverCollectService {

  private final NaverSearchPort naverSearchPort;
  private final NaverQueryCollector naverQueryCollector;
  private final BookRawPublishPort bookRawPublishPort;

  /**
   * 네이버 책 API 한 페이지만 조회하는 유스케이스입니다.
   *
   * <p>유효하지 않은 query(예: null 또는 공백 문자열)가 들어오면
   * 외부 API를 호출하지 않고 빈 결과를 반환합니다.
   *
   * @param query   검색어 (null 또는 공백일 경우 빈 결과 반환)
   * @param display 페이지당 개수 (null이면 infra에서 기본값 처리)
   * @param start   시작 인덱스 (null이면 infra에서 기본값 처리)
   * @param sort    정렬 기준 (null이면 infra에서 기본값 처리)
   * @return 네이버 API 한 페이지 조회 결과
   */
  public NaverSearchResponse searchSinglePage(
      String query,
      Integer display,
      Integer start,
      String sort
  ) {
    if (isBlankQuery(query)) {
      return emptyResponse();
    }

    return naverSearchPort.search(query, display, start, sort);
  }

  /**
   * 하나의 query에 대해 여러 페이지를 돌면서 모든 도서 데이터를 수집하는 유스케이스입니다.
   *
   * <p>페이징 로직 및 종료 조건(total, maxStart 등)은
   * {@link NaverQueryCollector}에 위임됩니다.
   *
   * @param query    검색어 (null 또는 공백일 경우 빈 리스트 반환)
   * @param maxStart 최대 start 값 (null이면 설정값 사용)
   * @return 해당 query로 수집된 모든 {@link NaverSearchItem} 리스트
   */
  public List<NaverSearchItem> collectAllByQuery(String query, Integer maxStart) {
    if (isBlankQuery(query)) {
      return Collections.emptyList();
    }
    return naverQueryCollector.collectAllByQuery(query, maxStart);
  }

  /**
   * 하나의 query에 대해 전체 페이징 수집을 수행한 뒤,
   * 수집된 결과를 외부 시스템(Kafka, RabbitMQ 등)으로 발행하는 유스케이스입니다.
   *
   * <p>수집 결과가 비어 있는 경우에는 발행을 수행하지 않습니다.
   *
   * @param query    검색어 (null 또는 공백일 경우 아무 작업도 수행하지 않음)
   * @param maxStart 최대 start 값 (null이면 설정값 사용)
   */
  public void collectAndPublishByQuery(String query, Integer maxStart) {
    if (isBlankQuery(query)) {
      return;
    }

    List<NaverSearchItem> items = naverQueryCollector.collectAllByQuery(query, maxStart);
    if (items.isEmpty()) {
      return;
    }

    bookRawPublishPort.publish(items);
  }

  private boolean isBlankQuery(String query) {
    return query == null || query.isBlank();
  }

  private NaverSearchResponse emptyResponse() {
    return new NaverSearchResponse(
        null,
        0,
        1,
        0,
        Collections.emptyList()
    );
  }
}
