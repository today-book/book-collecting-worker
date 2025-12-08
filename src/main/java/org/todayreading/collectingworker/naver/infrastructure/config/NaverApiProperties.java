package org.todayreading.collectingworker.naver.infrastructure.config;

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
@ConfigurationProperties(prefix = "naver")
public class NaverApiProperties {

  private BookProperties book = new BookProperties();
  private SearchProperties search = new SearchProperties();

  @Getter
  @Setter
  public static class BookProperties {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
  }

  @Getter
  @Setter
  public static class SearchProperties {

    /**
     * 한 번의 요청에서 가져올 결과 개수 (1~100).
     * application.yml 의 naver.search.display 에서 설정합니다.
     */
    private int display;

    /**
     * start 파라미터의 기본 최대값.
     * application.yml 의 naver.search.max-start 에서 설정합니다.
     */
    private int maxStart;

    /**
     * 일일 스캔(daily scan)에서 사용할 start 상한 값.
     * application.yml 의 naver.search.daily-max-start 에서 설정합니다.
     */
    private int dailyMaxStart;

    /**
     * 정렬 기준 (sim, date 등).
     * application.yml 의 naver.search.sort 에서 설정합니다.
     */
    private String sort;

    /**
     * 연속 호출 사이에 잠깐 쉬는 시간(ms).
     * 이 값은 필요하면 YAML에 추가하고, 없으면 기본값을 써도 된다.
     */
    private long requestIntervalMs = 200L;

    public Duration getRequestInterval() {
      return Duration.ofMillis(requestIntervalMs);
    }
  }
}
