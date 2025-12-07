package org.todayreading.collectingworker.naver.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Naver API(책 검색 등) 호출에 필요한 설정 정보를 보관하는 프로퍼티 클래스.
 *
 * <p>application.yml 의 {@code naver.book.*} 값을 바인딩해서 사용한다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "naver.book")
public class NaverApiProperties {

  private String baseUrl;
  private String clientId;
  private String clientSecret;


  private SearchProperties search = new SearchProperties();

  @Getter
  @Setter
  public static class SearchProperties {

    /**
     * 한 번의 요청에서 가져올 결과 개수 (1~100).
     */
    private int display = 100;

    /**
     * start 파라미터의 최대값.
     * (네이버 API 한계 내에서 안전하게 끊기 위한 용도)
     */
    private int maxStart = 1000;

    /**
     * 정렬 기준 (sim, date 등).
     */
    private String sort = "date";

    /**
     * 연속 호출 사이에 잠깐 쉬는 시간.
     */
    private long requestIntervalMs = 200L;

    /**
     * Duration 형태로도 쓰고 싶을 때 편하게 쓰는 헬퍼.
     */
    public Duration getRequestInterval() {
      return Duration.ofMillis(requestIntervalMs);
    }
  }
}
