package org.todayreading.collectingworker.naver.infrastructure.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.infrastructure.config.NaverApiProperties;

/**
 * 네이버 도서 검색 API를 호출하는 인프라 어댑터입니다.
 *
 * <p>{@link RestClient}로 `/v1/search/book.json` 엔드포인트를 호출하며,
 * 기본 파라미터 값은 {@link NaverApiProperties} 설정을 사용합니다.</p>
 *
 * <p>포트 {@link NaverSearchPort}의 구현체로 애플리케이션 계층에서
 * 외부 API 호출을 위임받습니다.</p>
 * <p>
 * @author 박성준
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NaverRestClient implements NaverSearchPort {

  private final RestClient naverBookRestClient;
  private final NaverApiProperties naverApiProperties;
  private static final String PATH = "/v1/search/book.json";


  /**
   * 네이버 도서 검색 API를 호출합니다.
   *
   * <p>display/start/sort가 null이면 설정값을 사용하며,
   * 응답이 null이면 {@link IllegalStateException}을 던집니다.</p>
   *
   * @param query   검색어
   * @param display 페이지당 개수 (null이면 설정값 사용)
   * @param start   시작 인덱스 (null이면 1부터)
   * @param sort    정렬 기준 (null이면 설정값 사용)
   * @return 네이버 검색 응답 DTO
   */
  @Override
  public NaverSearchResponse search(String query,
      Integer display,
      Integer start,
      String sort) {
    int actualDisplay =
        (display != null) ? display : naverApiProperties.getSearch().getDisplay();
    int actualStart = (start != null) ? start : 1;
    String actualSort =
        (sort != null) ? sort : naverApiProperties.getSearch().getSort();

    NaverSearchResponse response = naverBookRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(PATH)
            .queryParam("query", query)
            .queryParam("display", actualDisplay)
            .queryParam("start", actualStart)
            .queryParam("sort", actualSort)
            .build())
        .retrieve()
        .body(NaverSearchResponse.class);

    if (response == null) {
      // 추후에 공통 에러 처리
      throw new IllegalStateException("Naver API response is null.");
    }

    int itemCount = (response.items() == null) ? 0 : response.items().size();
    // 단일 호출 기준 응답 아이템 수와 요청 파라미터 로그
    // (sort는 기본값 사용 여부와 무관하게 생략)
    log.info(
        "Naver API call. query='{}' start={} display={} itemCount={}",
        query, actualStart, actualDisplay, itemCount);
    return response;
  }
}
