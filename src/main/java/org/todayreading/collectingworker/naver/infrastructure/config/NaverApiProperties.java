package org.todayreading.collectingworker.naver.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Naver API(책 검색 등) 호출에 필요한 설정 정보를 보관하는 프로퍼티 레코드입니다.
 *
 * <p>application.yml 의 {@code naver.*} 계층을 바인딩하여 사용합니다.</p>
 *
 * <p>구성 요소:</p>
 * <ul>
 *   <li>{@link #book} : Naver Book API 호출에 필요한 기본 URL 및 인증 정보</li>
 *   <li>{@link #search} : 검색 파라미터 및 호출 간격 등의 제어 설정</li>
 * </ul>
 *
 * @author 박성준
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "naver")
public record NaverApiProperties(
    BookProperties book,
    SearchProperties search
) {

  /**
   * Naver Book API 기본 설정 (base URL, 인증 정보 등)을 보관하는 레코드입니다.
   *
   * <p>RestClient/WebClient 생성 시 기본 URL 및 인증 헤더
   * ({@code X-Naver-Client-Id}, {@code X-Naver-Client-Secret})를 구성하는 데 사용됩니다.</p>
   */
  public record BookProperties(
      String baseUrl,    // naver.book.base-url
      String clientId,   // naver.book.client-id
      String clientSecret // naver.book.client-secret
  ) {
  }

  /**
   * Naver 책 검색 API 호출 시 사용할 검색 파라미터 및 호출 간격 설정을 보관하는 레코드입니다.
   *
   * <p>각 필드는 application.yml 의 {@code naver.search.*} 항목에 매핑됩니다.</p>
   * <ul>
   *   <li>{@code display} : 한 번의 요청에서 가져올 결과 개수 (1~100)</li>
   *   <li>{@code maxStart} : 풀스캔(full scan) 시 사용할 start 파라미터의 상한 값</li>
   *   <li>{@code sort} : 정렬 기준 (예: {@code sim}, {@code date})</li>
   *   <li>{@code requestIntervalMs} : 연속 호출 간 대기 시간(간격), 밀리초 단위</li>
   * </ul>
   */
  public record SearchProperties(
      int display,          // naver.search.display
      int maxStart,         // naver.search.max-start
      String sort,          // naver.search.sort
      long requestIntervalMs // naver.search.request-interval-ms (연속 호출 간 간격, ms 단위)
  ) {
  }
}
