package org.todayreading.collectingworker.naver.application.service;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.application.query.NaverQueryCollector;

/**
 * 네이버 책 검색 API를 단건 조회하거나,
 * 특정 검색어에 대해 수집만 수행하는 보조/점검용 애플리케이션 서비스입니다.
 *
 * <p>이 서비스는 전체 풀스캔/일일 스캔 배치 파이프라인과는 별도로,
 * 다음과 같은 용도로 사용하는 것을 목표로 합니다.
 * <ul>
 *   <li>단일 페이지 조회로 네이버 API 응답을 직접 확인</li>
 *   <li>특정 검색어에 대한 전체 수집 결과를 Kafka 발행 없이 확인</li>
 * </ul>
 *
 * <p>실제 배치 수집/발행 파이프라인은
 * {@link NaverCollectService}에서 담당합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class NaverSearchInspectService {

  private final NaverSearchPort naverSearchPort;
  private final NaverQueryCollector naverQueryCollector;

  /**
   * 네이버 책 API에서 한 페이지만 조회하는 보조 유스케이스입니다.
   *
   * <p>검색어가 {@code null} 이거나 공백이면 외부 API를 호출하지 않고
   * 빈 {@link NaverSearchResponse}를 반환합니다.
   *
   * @param query   검색어 (null 또는 공백일 경우 빈 결과 반환)
   * @param display 페이지당 개수 (null이면 infra에서 기본값 처리)
   * @param start   시작 인덱스 (null이면 infra에서 기본값 처리)
   * @param sort    정렬 기준 (null이면 infra에서 기본값 처리)
   * @return 네이버 API 한 페이지 조회 결과
   * @author 박성준
   * @since 1.0.0
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
   * 하나의 검색어에 대해 여러 페이지를 조회해 모든 도서 데이터를 수집하는
   * 보조 유스케이스입니다.
   *
   * <p>수집된 결과는 반환만 하며, Kafka 등 외부 시스템으로 발행하지 않습니다.
   * 페이징 처리와 종료 조건(total, maxStart 등)은
   * {@link NaverQueryCollector}에 위임합니다.
   *
   * @param query    검색어 (null 또는 공백일 경우 빈 리스트 반환)
   * @param maxStart 최대 start 값 (null이면 설정값 사용)
   * @return 해당 검색어로 수집된 모든 {@link NaverSearchItem} 리스트
   * @author 박성준
   * @since 1.0.0
   */
  public List<NaverSearchItem> collectAllByQuery(String query, Integer maxStart) {
    if (isBlankQuery(query)) {
      return Collections.emptyList();
    }
    return naverQueryCollector.collectAllByQuery(query, maxStart);
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

  /**
   * 외부 API를 호출하지 않았을 때 사용할 빈 응답 객체를 생성합니다.
   *
   * @return 아이템이 비어 있는 기본 {@link NaverSearchResponse}
   * @author 박성준
   * @since 1.0.0
   */
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
