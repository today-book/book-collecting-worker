package org.todayreading.collectingworker.naver.infrastructure.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.todayreading.collectingworker.naver.application.port.out.NaverSearchPort;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.infrastructure.config.NaverApiProperties;

@Component
@RequiredArgsConstructor
public class NaverRestClient implements NaverSearchPort {

  private final RestClient naverRestClient;
  private final NaverApiProperties naverApiProperties;
  private static final String PATH = "/v1/search/book.json";


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

    return naverRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(PATH)
            .queryParam("query", query)
            .queryParam("display", actualDisplay)
            .queryParam("start", actualStart)
            .queryParam("sort", actualSort)
            .build())
        .retrieve()
        .body(NaverSearchResponse.class);
  }
}
