package org.todayreading.collectingworker.naver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.todayreading.collectingworker.naver.config.NaverApiProperties;
import org.todayreading.collectingworker.naver.dto.NaverSearchResponse;

/**
 * Naver 책 검색 API를 호출하는 클라이언트 클래스이다.
 *
 * <p>{@link RestClient}를 이용해 {@code /v1/search/book.json} 엔드포인트를 호출하고,
 * 응답을 {@link NaverSearchResponse}로 디시리얼라이즈한다.</p>
 *
 * <p>검색 파라미터의 기본값은 {@link NaverApiProperties.SearchProperties}에 정의된 설정을 사용하며,
 * 호출 시 명시적으로 전달된 값이 있다면 그 값을 우선한다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class NaverClient {

  private final RestClient naverRestClient;
  private final NaverApiProperties naverApiProperties;

  /**
   * Naver 책 검색 API 엔드포인트 경로 상수.
   *
   * <p>base-url이 {@code https://openapi.naver.com} 인 경우,
   * 최종 호출 URL은 {@code https://openapi.naver.com/v1/search/book.json} 이 된다.</p>
   */
  private static final String SEARCH_BOOK_PATH = "/v1/search/book.json";

  /**
   * Naver 책 검색 API를 호출하여 도서 검색 결과를 조회한다.
   *
   * <p>{@code display}, {@code start}, {@code sort} 파라미터가 {@code null} 인 경우에는
   * {@link NaverApiProperties.SearchProperties}에 정의된 기본값을 사용한다.</p>
   *
   * @param query   검색어 (도서 제목, 키워드 등)
   * @param display 한 번에 조회할 결과 수. {@code null} 인 경우 설정된 기본값을 사용한다.
   * @param start   검색 시작 위치(페이지 시작 인덱스). {@code null} 인 경우 1로 간주한다.
   * @param sort    정렬 기준(sim, date 등). {@code null} 인 경우 설정된 기본값을 사용한다.
   * @return Naver 책 검색 API 응답을 표현하는 {@link NaverSearchResponse}
   */
  public NaverSearchResponse searchBooks(
      String query,
      Integer display,
      Integer start,
      String sort
  ) {
    int actualDisplay =
        (display != null) ? display : naverApiProperties.getSearch().getDisplay();
    int actualStart = (start != null) ? start : 1;
    String actualSort =
        (sort != null) ? sort : naverApiProperties.getSearch().getSort();

    return naverRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(SEARCH_BOOK_PATH)
            .queryParam("query", query)
            .queryParam("display", actualDisplay)
            .queryParam("start", actualStart)
            .queryParam("sort", actualSort)
            .build())
        .retrieve()
        .body(NaverSearchResponse.class);
  }
}
