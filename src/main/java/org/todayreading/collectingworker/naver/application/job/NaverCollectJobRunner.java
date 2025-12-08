package org.todayreading.collectingworker.naver.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.naver.application.service.NaverCollectService;

/**
 * 네이버 도서 수집 배치(full/daily)를 비동기로 실행하기 위한 잡 실행기입니다.
 *
 * <p>이 클래스는 HTTP 컨트롤러나 스케줄러에서 호출되며,
 * 실제 배치 유스케이스 로직은 {@link NaverCollectService}에 위임합니다.
 * 내부 메서드는 {@link Async} 애노테이션을 사용해 별도의 스레드 풀에서 실행됩니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverCollectJobRunner {

  private final NaverCollectService naverCollectService;

  /**
   * 네이버 도서 전체 풀스캔 배치를 비동기로 실행합니다.
   *
   * <p>실제 수집/발행 로직은 {@link NaverCollectService#fullScanAndPublish()}에 위임합니다.</p>
   *
   * @author 박성준
   * @since 1.0.0
   */
  @Async("naverBatchExecutor")
  public void runFullScanAsync() {
    try {
      naverCollectService.fullScanAndPublish();
    } catch (Exception ex) {
      log.error("Async job failed: Naver full scan.", ex);
    }
  }

  /**
   * 네이버 도서 일일 스캔 배치를 비동기로 실행합니다.
   *
   * <p>{@code maxStart}가 {@code null}이면
   * {@link NaverCollectService#dailyScanAndPublish()}를 호출하고,
   * 명시된 값이 있으면 {@link NaverCollectService#dailyScanAndPublish()} (Integer)}를 호출합니다.</p>
   *
   * @param maxStart 일일 스캔에서 사용할 최대 start 값 (null 이면 설정값 사용)
   * @author 박성준
   * @since 1.0.0
   */
  @Async("naverBatchExecutor")
  public void runDailyScanAsync(Integer maxStart) {
    try {
      if (maxStart == null) {
        naverCollectService.dailyScanAndPublish();
      } else {
        naverCollectService.dailyScanAndPublish(maxStart);
      }
    } catch (Exception ex) {
      log.error("Async job failed: Naver daily scan. maxStart={}", maxStart, ex);
    }
  }
}
