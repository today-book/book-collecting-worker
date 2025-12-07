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
 * 단일 검색어(query)에 대해 네이버 책 검색 API를 페이징으로 수집하는 컴포넌트입니다.
 *
 * <p>이 컴포넌트는 {@link NaverSearchPort}를 통해 외부 네이버 Open API를 호출하고,
 * {@link NaverApiProperties}에 정의된 설정(maxStart 등)을 이용해 수집 범위를 제어합니다.
 *
 * <p>현재는 {@link #collectAllByQuery(String, Integer)} 메서드를 통해
 * 하나의 query에 대해 가능한 범위까지 모든 {@link NaverSearchItem}을 수집하는 역할만 담당하며,
 * 상위 유스케이스(예: 패턴 기반 풀스캔, Kafka/RabbitMQ 발행)는
 * 별도의 서비스 계층에서 오케스트레이션하는 것을 전제로 합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class NaverQueryCollector {

  /**
   * 네이버 책 검색 API를 호출하기 위한 포트 인터페이스입니다.
   *
   * <p>실제 HTTP 호출 구현체는 infrastructure 레이어에서 제공되며,
   * 이 컴포넌트는 포트를 통해서만 외부 API에 접근합니다.
   */
  private final NaverSearchPort naverSearchPort;

  /**
   * 네이버 API 호출 시 사용할 검색 관련 설정(display, maxStart 등)을 제공하는 프로퍼티입니다.
   *
   * <p>현재는 최대 시작 인덱스(maxStart)를 가져오기 위해 사용됩니다.
   */
  private final NaverApiProperties naverApiProperties;

  /**
   * 하나의 검색어(query)에 대해 네이버 책 검색 API를 여러 페이지에 걸쳐 호출하고,
   * 수집된 모든 {@link NaverSearchItem}을 리스트로 반환합니다.
   *
   * <p>동작 방식은 다음과 같습니다:
   * <ol>
   *   <li>유효하지 않은 query(null 또는 공백)인 경우, 즉시 빈 리스트를 반환합니다.</li>
   *   <li>{@code maxStart}가 지정되어 있으면 해당 값을, 그렇지 않으면
   *   {@link NaverApiProperties}의 설정 값을 기반으로 수집 상한을 계산합니다.</li>
   *   <li>{@code start} 값을 1부터 시작하여 {@link NaverSearchPort#search(String, Integer, Integer, String)}
   *   를 호출하고, 각 페이지의 {@code items}를 결과 리스트에 누적합니다.</li>
   *   <li>다음 페이지의 {@code start} 값은 응답의 {@code display}만큼 증가시키며,
   *   더 이상 아이템이 없거나 {@code total} 또는 설정된 상한을 초과하면 루프를 종료합니다.</li>
   * </ol>
   *
   * @param query    네이버 API에 전달할 검색어 (null 또는 공백이면 빈 리스트 반환)
   * @param maxStart 최대 start 값 (null이면 설정값 사용)
   * @return 해당 query로 수집된 모든 {@link NaverSearchItem} 리스트
   * @author 박성준
   * @since 1.0.0
   */
  public List<NaverSearchItem> collectAllByQuery(String query, Integer maxStart) {
    if (query == null || query.isBlank()) {
      return Collections.emptyList();
    }

    int limitStart =
        (maxStart != null) ? maxStart : naverApiProperties.getSearch().getMaxStart();

    int start = 1;
    List<NaverSearchItem> allItems = new ArrayList<>();

    while (start <= limitStart) {
      NaverSearchResponse response = naverSearchPort.search(query, null, start, null);

      if (response.items() == null || response.items().isEmpty()) {
        break;
      }

      allItems.addAll(response.items());

      int pageSize = response.display();
      if (pageSize <= 0) {
        break;
      }

      start += pageSize;

      if (start > response.total()) {
        break;
      }
    }

    return allItems;
  }
}
