package org.todayreading.collectingworker.naver.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;
import org.todayreading.collectingworker.naver.application.service.NaverSearchInspectService;

import java.util.List;

@RestController
@RequestMapping("/api/naver/inspect")
@RequiredArgsConstructor
public class NaverSearchInspectController {

  private final NaverSearchInspectService naverSearchInspectService;

  /**
   * 단일 페이지 조회용 엔드포인트.
   * 예) GET /api/naver/inspect/single?query=자바&display=10&start=1&sort=sim
   */
  @GetMapping("/single")
  public NaverSearchResponse searchSinglePage(
      @RequestParam String query,
      @RequestParam(required = false) Integer display,
      @RequestParam(required = false) Integer start,
      @RequestParam(required = false) String sort
  ) {
    return naverSearchInspectService.searchSinglePage(query, display, start, sort);
  }

  /**
   * 한 쿼리에 대해 여러 페이지를 수집해서 아이템 리스트만 모아서 보고 싶은 경우.
   * 예) GET /api/naver/inspect/collect?query=자바&maxPages=3
   */
  @GetMapping("/collect")
  public List<NaverSearchItem> collectAllByQuery(
      @RequestParam String query,
      @RequestParam(defaultValue = "3") Integer maxPages
  ) {
    return naverSearchInspectService.collectAllByQuery(query, maxPages);
  }
}
