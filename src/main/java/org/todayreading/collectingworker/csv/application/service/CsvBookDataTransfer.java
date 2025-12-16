package org.todayreading.collectingworker.csv.application.service;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.csv.application.port.out.CsvBookPublishPort;
import org.todayreading.collectingworker.csv.application.port.out.CsvFileReadPort;
import org.todayreading.collectingworker.csv.application.service.command.CsvTransferCommand;

/**
 * CSV 입력(파일/디렉터리)을 읽어 라인 발행을 오케스트레이션하는 애플리케이션 서비스입니다.
 *
 * <p>이 클래스는 "흐름(오케스트레이션)"만 담당합니다.</p>
 * <ul>
 *   <li>CSV 읽기: {@link CsvFileReadPort}가 담당(인프라 구현)</li>
 *   <li>라인 발행: {@link CsvBookPublishPort}가 담당(인프라 구현)</li>
 *   <li>스킵/집계/파일별 요약: {@link CsvTransferListener}가 담당(정책/처리)</li>
 * </ul>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvBookDataTransfer {

  /** CSV 원본 라인을 외부(Kafka 등)로 발행하는 출력 포트입니다. */
  private final CsvBookPublishPort csvBookPublishPort;

  /** CSV 파일/디렉터리를 읽어 파일 단위 이벤트로 전달하는 출력 포트입니다. */
  private final CsvFileReadPort csvFileReadPort;

  /**
   * CSV 전송을 실행합니다.
   *
   * <p>처리 흐름:</p>
   * <ol>
   *   <li>입력 경로를 커맨드에서 조회</li>
   *   <li>통계 누적 객체({@link TransferStats}) 생성</li>
   *   <li>이벤트 리스너({@link CsvTransferListener}) 생성</li>
   *   <li>{@link CsvFileReadPort}를 통해 파일을 읽고, 이벤트를 리스너로 전달</li>
   *   <li>처리 종료 후 전체 요약 로그 출력</li>
   * </ol>
   *
   * @param command CSV 전송 입력 커맨드(파일 또는 디렉터리 경로)
   */
  public void transfer(CsvTransferCommand command) {
    // 유스케이스 입력(파일/디렉터리 경로)
    Path inputPath = command.inputPath();

    // 실행 시작 로그(전체 1회)
    log.info("CSV 전송 시작. inputPath={}", inputPath);

    // 파일별/전체 통계 누적 객체
    TransferStats stats = new TransferStats();

    // 파일 이벤트(onFileStart/onLine/onFileEnd)를 받아 스킵/발행/집계를 처리하는 리스너
    CsvTransferListener listener = new CsvTransferListener(csvBookPublishPort, stats);

    // 실제 파일 읽기/디렉터리 순회는 포트 구현체(인프라)가 수행하고,
    // 읽기 과정에서 발생하는 이벤트를 listener로 전달합니다.
    csvFileReadPort.read(inputPath, listener);

    // 전체 합계 요약 로그(전체 1회)
    logOverallSummary(inputPath, stats);
  }

  /**
   * 실행 전체에 대한 요약 로그를 출력합니다.
   *
   * <p>파일별 요약은 {@link CsvTransferListener}에서 출력하고,
   * 여기서는 전체 합계만 출력합니다.</p>
   */
  private void logOverallSummary(Path inputPath, TransferStats stats) {
    log.info(
        "CSV 전송 완료. inputPath={}, fileCount={}, totalLines={}, skippedLines={}, publishedLines={}, failedLines={}",
        inputPath,
        stats.fileCount(),
        stats.totalLines(),
        stats.skippedLines(),
        stats.publishedLines(),
        stats.failedLines()
    );
  }
}
