package org.todayreading.collectingworker.naver.application.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 네이버 도서 일일 수집 배치를 스케줄링하기 위한 컴포넌트입니다.
 *
 * <p>매일 새벽 2시에 {@link NaverCollectJobRunner}를 통해
 * 일일 스캔 배치를 비동기로 실행합니다.</p>
 *
 * <p>풀스캔(full scan)은 스케줄러에서 절대 호출하지 않으며,
 * 운영자가 수동으로 API를 호출할 때만 실행됩니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class NaverCollectScheduler {

  private final NaverCollectJobRunner jobRunner;

  /**
   * 매일 새벽 2시에 일일 스캔 배치를 비동기로 실행합니다.
   *
   * <p>{@code maxStart} 값은 {@code null}로 전달하여,
   * {@link org.todayreading.collectingworker.naver.application.service.NaverCollectService#dailyScanAndPublish()}
   * 내부에서 설정값(daily-max-start)을 사용하도록 위임합니다.</p>
   *
   * <p>실제 유스케이스 로직은 {@link NaverCollectJobRunner} 및
   * {@link org.todayreading.collectingworker.naver.application.service.NaverCollectService}에서
   * 처리합니다.</p>
   *
   * @author 박성준
   * @since 1.0.0
   */
//  @Scheduled(cron = "0 * * * * *") // 매 분 0초마다 (테스트용)
  @Scheduled(cron = "0 0 2 * * *")
  public void runDailyScanAt2AM() {
    jobRunner.runDailyScanAsync(null);
  }
}
