package org.todayreading.collectingworker.naver.application.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 네이버 도서 풀스캔 배치를 스케줄링하기 위한 컴포넌트입니다.
 *
 * <p>매일 새벽 1시에 {@link NaverCollectJobRunner}를 통해
 * 풀스캔 배치를 비동기로 실행합니다.</p>
 *
 * <p>수동 실행은 컨트롤러를 통해 계속 지원되며,
 * 스케줄러는 매일 자동 풀스캔만 수행합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class NaverCollectScheduler {

  private final NaverCollectJobRunner jobRunner;

  /**
   * 매일 새벽 1시에 풀스캔 배치를 비동기로 실행합니다.
   *
   * <p>실제 유스케이스 로직은 {@link NaverCollectJobRunner} 및
   * {@link org.todayreading.collectingworker.naver.application.service.NaverCollectService}에서
   * 처리합니다.</p>
   *
   * @author 박성준
   * @since 1.0.0
   */
//  @Scheduled(cron = "0 * * * * *") // 매 분 0초마다 (테스트용)
  @Scheduled(cron = "0 0 1 * * *")
  public void runFullScanAt2AM() {
    jobRunner.runFullScanAsync();
  }
}
