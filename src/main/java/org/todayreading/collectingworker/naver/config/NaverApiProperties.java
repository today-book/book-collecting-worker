package org.todayreading.collectingworker.naver.config;

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


}
