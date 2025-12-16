package org.todayreading.collectingworker.csv.application.service;

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.todayreading.collectingworker.csv.application.port.out.CsvBookPublishPort;
import org.todayreading.collectingworker.csv.application.port.out.CsvFileReadPort.CsvFileReadListenerAdapter;

/**
 * {@link org.todayreading.collectingworker.csv.application.port.out.CsvFileReadPort}가 발생시키는
 * 파일 단위 이벤트(onFileStart/onLine/onFileEnd)를 받아,
 * 스킵(헤더/공백) 정책 적용 → 발행(publish) → 통계 누적 → 파일별 요약 로그를 처리하는 리스너입니다.
 *
 * <p>처리 기준:</p>
 * <ul>
 *   <li>헤더: {@code lineNumber == 1} (포트가 파일별 라인 번호를 1부터 시작한다는 계약 전제)</li>
 *   <li>공백: {@code line == null || line.isBlank()}</li>
 * </ul>
 *
 * <p>통계 의미:</p>
 * <ul>
 *   <li>publishedLines: {@code publish()} 호출이 예외 없이 끝난 라인 수</li>
 *   <li>failedLines: {@code publish()} 호출 중 예외가 발생한 라인 수</li>
 * </ul>
 *
 * <p>주의: Kafka 전송이 비동기인 경우, 실제 전송 실패는 KafkaTemplate 콜백/로그에서 확인해야 합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
final class CsvTransferListener extends CsvFileReadListenerAdapter {

  /** CSV 원본 라인을 외부(Kafka 등)로 발행하는 출력 포트입니다. */
  private final CsvBookPublishPort publishPort;

  /** 파일별/전체 통계를 누적하는 상태 객체입니다. */
  private final TransferStats stats;

  /**
   * 리스너를 생성합니다.
   *
   * @param publishPort 라인 발행을 수행할 출력 포트
   * @param stats       통계를 누적할 상태 객체
   */
  CsvTransferListener(CsvBookPublishPort publishPort, TransferStats stats) {
    this.publishPort = publishPort;
    this.stats = stats;
  }

  /**
   * 파일 읽기 시작 이벤트입니다.
   *
   * <p>라인이 한 번도 발생하지 않는 "빈 파일"도 집계에 포함시키기 위해
   * 파일 시작 시점에 통계 객체를 미리 등록합니다.</p>
   *
   * @param filePath 읽기 시작한 파일 경로
   */
  @Override
  public void onFileStart(Path filePath) {
    // 빈 파일(0라인)도 fileCount/요약에 포함되도록 등록합니다.
    stats.ensureFile(filePath);

    // 파일 시작 로그는 기본(info)에서는 줄이고, 필요할 때만 debug로 확인합니다.
    if (log.isDebugEnabled()) {
      log.debug("CSV 파일 처리 시작. filePath={}", filePath);
    }
  }

  /**
   * 파일에서 한 라인을 읽을 때마다 호출되는 이벤트입니다.
   *
   * <p>처리 순서:</p>
   * <ol>
   *   <li>파일별 totalLines 갱신(최대 lineNumber)</li>
   *   <li>헤더/공백 라인 스킵 → skippedLines 증가</li>
   *   <li>정상 라인 발행 → 성공 시 publishedLines, 실패 시 failedLines 증가</li>
   * </ol>
   *
   * @param filePath   현재 파일 경로
   * @param lineNumber 파일 내 라인 번호(1부터 시작)
   * @param line       CSV 원본 라인(개행 제외)
   */
  @Override
  public void onLine(Path filePath, int lineNumber, String line) {
    // "최대 lineNumber"를 totalLines로 사용합니다.
    stats.updateTotalLines(filePath, lineNumber);

    // 헤더(1행) 또는 공백 라인은 발행하지 않습니다.
    if (isHeaderOrBlank(lineNumber, line)) {
      stats.incrementSkipped(filePath);
      return;
    }

    // 라인 발행 실패가 전체 실행을 중단시키지 않도록 예외를 흡수합니다.
    if (publishSafely(filePath, lineNumber, line)) {
      stats.incrementPublished(filePath);
    } else {
      stats.incrementFailed(filePath);
    }
  }

  /**
   * 파일 읽기 종료 이벤트입니다.
   *
   * <p>파일 단위 요약 로그를 1줄로 출력합니다.</p>
   *
   * @param filePath 읽기 종료한 파일 경로
   */
  @Override
  public void onFileEnd(Path filePath) {
    FileStats s = stats.get(filePath);
    log.info(
        "CSV 파일 요약. filePath={}, totalLines={}, skippedLines={}, publishedLines={}, failedLines={}",
        filePath, s.totalLines, s.skippedLines, s.publishedLines, s.failedLines
    );
  }

  /**
   * 헤더(1행) 또는 공백 라인인지 판단합니다.
   *
   * @param lineNumber 파일 내 라인 번호
   * @param line       CSV 원본 라인
   * @return 스킵 대상이면 true
   */
  private boolean isHeaderOrBlank(int lineNumber, String line) {
    return lineNumber == 1 || line == null || line.isBlank();
  }

  /**
   * 라인을 발행하되, 예외는 잡아서 경고 로그만 남기고 실패로 처리합니다.
   *
   * @param filePath   파일 경로(로그용)
   * @param lineNumber 라인 번호(로그용)
   * @param line       발행할 CSV 원본 라인
   * @return 성공이면 true, 예외 발생이면 false
   */
  private boolean publishSafely(Path filePath, int lineNumber, String line) {
    try {
      publishPort.publish(line);
      return true;
    } catch (Exception e) {
      log.warn("CSV 라인 발행 실패. filePath={}, lineNumber={}", filePath, lineNumber, e);
      return false;
    }
  }
}
