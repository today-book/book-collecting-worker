package org.todayreading.collectingworker.naver.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Naver API 호출에 사용할 {@link RestClient} 설정 클래스.
 *
 * <p>기본 URL과 인증 헤더(Naver Client Id/Secret)가 미리 설정된 전용 RestClient를 생성한다.</p>
 *
 * <p>책 검색 API뿐 아니라 추후 다른 Naver API 확장에도 재사용할 수 있다.</p>
 *
 * author 박성준
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(NaverApiProperties.class)
public class NaverRestClientConfig {

  /**
   * Naver API 전용 RestClient Bean.
   *
   * @param properties Naver API 설정 값
   * @param builder    Spring에서 주입해 주는 RestClient 빌더
   * @return Naver API 호출에 사용되는 RestClient
   */
  @Bean
  public RestClient naverBookRestClient(
      NaverApiProperties properties,
      RestClient.Builder builder
  ) {

    NaverApiProperties.BookProperties bookProps = properties.book();
    return builder
        .baseUrl(bookProps.baseUrl())
        .defaultHeader("X-Naver-Client-Id", bookProps.clientId())
        .defaultHeader("X-Naver-Client-Secret", bookProps.clientSecret())
        .build();
  }
}
