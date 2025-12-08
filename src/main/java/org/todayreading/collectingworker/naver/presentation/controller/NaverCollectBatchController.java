package org.todayreading.collectingworker.naver.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.todayreading.collectingworker.naver.application.service.NaverCollectService;

/**
 * 네이버 도서 수집 배치를 수동으로 트리거하기 위한 컨트롤러입니다.
 *
 * <p>이 컨트롤러는 운영자/관리자 등 내부 사용자를 대상으로 하며,
 * 다음과 같은 배치 유스케이스를 HTTP 호출로 실행할 수 있게 합니다.
 * <ul>
 *   <li>전체 풀스캔 배치: {@link #triggerFullScan()}</li>
 *   <li>일일 스캔 배치: {@link #triggerDailyScan(Integer)}</li>
 * </ul>
 *
 * <p>실제 수집/발행 로직은 {@link NaverCollectService}에 위임됩니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/internal/naver/collect")
@RequiredArgsConstructor
public class NaverCollectBatchController {

  private final NaverCollectService naverCollectService;

  /**
   * 네이버 도서 전체 풀스캔 배치를 즉시 실행합니다.
   *
   * <p>application.yml 의 {@code naver.search.max-start} 설정을 사용하여
   * 각 검색어에 대해 가능한 범위까지 페이징 수집을 수행한 뒤,
   * 수집된 결과를 Kafka(book.raw 등)로 발행합니다.</p>
   *
   * <p>실행 시간이 길어질 수 있으므로, 응답 코드는 {@code 202 Accepted}를 반환합니다.</p>
   *
   * @return 수집 작업이 접수되었음을 나타내는 HTTP 202 응답
   * @author 박성준
   * @since 1.0.0
   */
  @PostMapping("/full-scan")
  public ResponseEntity<Void> triggerFullScan() {
    log.info("Manual trigger: Naver full scan started.");
    // 현재는 동기 실행이지만, 향후 비동기/Job 시스템으로 변경할 여지를 남겨둡니다.
    naverCollectService.fullScanAndPublish();
    log.info("Manual trigger: Naver full scan finished.");
    return ResponseEntity.accepted().build();
  }

  /**
   * 네이버 도서 일일 스캔 배치를 즉시 실행합니다.
   *
   * <p>요청 파라미터 {@code maxStart}가 지정되지 않으면,
   * application.yml 의 {@code naver.search.daily-max-start} 설정값을 사용합니다.</p>
   *
   * <p>일일 스캔은 전체 풀스캔보다 가벼운 용도로 설계되었으며,
   * 요일별 쿼리 패턴 분배 + 제한된 start 범위 내에서만 수집을 수행합니다.</p>
   *
   * @param maxStart 일일 스캔에서 사용할 최대 start 값(옵션).
   *                 {@code null}인 경우 설정값(daily-max-start)을 사용합니다.
   * @return 수집 작업이 접수되었음을 나타내는 HTTP 202 응답
   * @author 박성준
   * @since 1.0.0
   */
  @PostMapping("/daily-scan")
  public ResponseEntity<Void> triggerDailyScan(
      @RequestParam(name = "maxStart", required = false) Integer maxStart
  ) {
    if (maxStart == null) {
      log.info("Manual trigger: Naver daily scan started. (use configured daily-max-start)");
      naverCollectService.dailyScanAndPublish();
    } else {
      log.info("Manual trigger: Naver daily scan started with custom maxStart={}.", maxStart);
      naverCollectService.dailyScanAndPublish(maxStart);
    }
    log.info("Manual trigger: Naver daily scan finished.");
    return ResponseEntity.accepted().build();
  }
}
