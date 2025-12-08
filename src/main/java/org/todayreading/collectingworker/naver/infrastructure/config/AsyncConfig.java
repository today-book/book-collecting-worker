package org.todayreading.collectingworker.naver.infrastructure.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 네이버 수집 배치 작업을 비동기 및 스케줄링 방식으로 실행하기 위한 설정입니다.
 *
 * <p>다음 컴포넌트에서 이 설정을 사용합니다:
 * <ul>
 *   <li>{@code @Async("naverBatchExecutor")}를 사용하는
 *   {@link org.todayreading.collectingworker.naver.application.job.NaverCollectJobRunner}</li>
 *   <li>{@code @Scheduled}를 사용하는
 *   {@link org.todayreading.collectingworker.naver.application.job.NaverCollectScheduler}</li>
 * </ul>
 *
 * @author 박성준
 * @since 1.0.0
 */
@EnableAsync
@EnableScheduling
@Configuration
public class AsyncConfig {

  /**
   * 네이버 배치 작업용 전용 스레드 풀입니다.
   *
   * <p>{@code @Async("naverBatchExecutor")}로 지정된 비동기 작업들이
   * 이 Executor에서 실행됩니다.</p>
   *
   * @return 네이버 배치 작업 실행에 사용할 Executor
   * @author 박성준
   * @since 1.0.0
   */
  @Bean(name = "naverBatchExecutor")
  public Executor naverBatchExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("naver-batch-");
    executor.initialize();
    return executor;
  }
}
