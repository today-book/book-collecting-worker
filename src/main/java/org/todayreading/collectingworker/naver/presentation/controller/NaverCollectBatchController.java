package org.todayreading.collectingworker.naver.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.todayreading.collectingworker.naver.application.job.NaverCollectJobRunner;

/**
 * 네이버 도서 수집 배치를 수동으로 트리거하기 위한 컨트롤러입니다.
 *
 * <p>이 컨트롤러는 운영자/관리자 등 내부 사용자를 대상으로 하며,
 * HTTP 요청을 통해 네이버 수집 배치를 비동기적으로 실행합니다.
 *
 * <p>실제 수집/발행 유스케이스 로직은
 * {@link org.todayreading.collectingworker.naver.application.service.NaverCollectService}에 있으며,
 * 이 컨트롤러는 {@link NaverCollectJobRunner}를 통해 비동기 실행만 트리거합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/naver/collect")
@RequiredArgsConstructor
public class NaverCollectBatchController {

  private final NaverCollectJobRunner jobRunner;

  /**
   * 네이버 도서 전체 풀스캔 배치를 비동기로 실행합니다.
   *
   * <p>요청이 들어오면 즉시 비동기 작업을 트리거하고,
   * 배치 완료 여부와 관계없이 {@code 202 Accepted}를 반환합니다.</p>
   *
   * @return 수집 작업이 비동기로 접수되었음을 나타내는 HTTP 202 응답
   * @author 박성준
   * @since 1.0.0
   */
  @PostMapping("/full-scan")
  public ResponseEntity<Void> triggerFullScan() {
    jobRunner.runFullScanAsync();
    return ResponseEntity.accepted().build();
  }

  /**
   * 네이버 도서 일일 스캔 배치를 비동기로 실행합니다.
   *
   * <p>{@code maxStart} 파라미터가 없으면 설정값(daily-max-start)을 사용하고,
   * 값이 지정된 경우 해당 최대 start 범위까지만 일일 스캔을 수행합니다.
   * 요청은 배치 완료 여부와 관계없이 {@code 202 Accepted}를 즉시 반환합니다.</p>
   *
   * @param maxStart 일일 스캔에서 사용할 최대 start 값(옵션, null 허용)
   * @return 수집 작업이 비동기로 접수되었음을 나타내는 HTTP 202 응답
   * @author 박성준
   * @since 1.0.0
   */
  @PostMapping("/daily-scan")
  public ResponseEntity<Void> triggerDailyScan(
      @RequestParam(name = "maxStart", required = false) Integer maxStart
  ) {
    jobRunner.runDailyScanAsync(maxStart);
    return ResponseEntity.accepted().build();
  }
}
