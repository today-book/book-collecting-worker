package org.todayreading.collectingworker.csv.application.service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CSV 전송 실행 중 파일별 통계를 누적하는 상태 객체입니다.
 *
 * <p>이 클래스는 "집계 전용"으로 사용되며, 파일 읽기/발행 로직은 포함하지 않습니다.</p>
 *
 * <p>설계 포인트:</p>
 * <ul>
 *   <li>{@link LinkedHashMap}을 사용하여 파일이 등록된 순서를 유지합니다.
 *       (파일별 요약 로그 출력 시 일관된 순서를 확보)</li>
 *   <li>빈 파일(0라인)도 집계에 포함시키기 위해 {@link #ensureFile(Path)}를 제공합니다.</li>
 * </ul>
 *
 * @author 박성준
 * @since 1.0.0
 */
final class TransferStats {

  /**
   * 파일 경로별 통계를 보관합니다.
   *
   * <p>키: 처리 중인 CSV 파일 경로</p>
   * <p>값: 해당 파일의 집계 정보</p>
   */
  private final Map<Path, FileStats> statsByFile = new LinkedHashMap<>();

  /**
   * 파일 통계가 없으면 생성하여 등록합니다.
   *
   * <p>빈 파일처럼 라인 이벤트(onLine)가 한 번도 발생하지 않는 케이스에서도
   * fileCount에 포함시키기 위해 onFileStart 시점에 호출하는 용도입니다.</p>
   *
   * @param filePath 처리 중인 파일 경로
   */
  void ensureFile(Path filePath) {
    statsByFile.putIfAbsent(filePath, new FileStats());
  }

  /**
   * 파일 통계를 조회합니다. 없으면 생성합니다.
   *
   * <p>방어적으로 {@code computeIfAbsent}를 사용하여,
   * 예외적으로 onLine이 onFileStart보다 먼저 호출되는 상황에서도 NPE 없이 동작하도록 합니다.</p>
   *
   * @param filePath 처리 중인 파일 경로
   * @return 해당 파일의 통계 객체
   */
  FileStats get(Path filePath) {
    return statsByFile.computeIfAbsent(filePath, ignored -> new FileStats());
  }

  /**
   * 파일별 총 라인 수를 갱신합니다.
   *
   * <p>현재 구현에서는 "최대 lineNumber"를 totalLines로 사용합니다.
   * (포트가 파일별 lineNumber를 1부터 증가시키는 계약을 전제로 합니다.)</p>
   *
   * @param filePath   처리 중인 파일 경로
   * @param lineNumber 현재 라인 번호(1부터 시작)
   */
  void updateTotalLines(Path filePath, int lineNumber) {
    FileStats s = get(filePath);
    s.totalLines = Math.max(s.totalLines, lineNumber);
  }

  /**
   * 파일별 스킵 라인 수를 1 증가시킵니다.
   *
   * <p>예: 헤더(1행), 공백 라인 등</p>
   *
   * @param filePath 처리 중인 파일 경로
   */
  void incrementSkipped(Path filePath) {
    get(filePath).skippedLines++;
  }

  /**
   * 파일별 발행 성공 라인 수를 1 증가시킵니다.
   *
   * <p>주의: publish() 호출이 예외 없이 끝난 기준이며,
   * Kafka 비동기 전송 실패는 별도 콜백 로그에서 확인해야 합니다.</p>
   *
   * @param filePath 처리 중인 파일 경로
   */
  void incrementPublished(Path filePath) {
    get(filePath).publishedLines++;
  }

  /**
   * 파일별 발행 실패 라인 수를 1 증가시킵니다.
   *
   * <p>publish() 호출 중 예외가 발생한 경우에만 증가합니다.</p>
   *
   * @param filePath 처리 중인 파일 경로
   */
  void incrementFailed(Path filePath) {
    get(filePath).failedLines++;
  }

  /**
   * 집계 대상 파일 수를 반환합니다.
   *
   * @return 집계된 파일 수
   */
  int fileCount() {
    return statsByFile.size();
  }

  /**
   * 총 라인 수(파일별 totalLines 합)를 반환합니다.
   *
   * @return 총 라인 수
   */
  long totalLines() {
    return statsByFile.values().stream().mapToLong(s -> s.totalLines).sum();
  }

  /**
   * 스킵된 라인 수 총합을 반환합니다.
   *
   * @return 스킵된 라인 수 총합
   */
  long skippedLines() {
    return statsByFile.values().stream().mapToLong(s -> s.skippedLines).sum();
  }

  /**
   * 발행 성공 라인 수 총합을 반환합니다.
   *
   * @return 발행 성공 라인 수 총합
   */
  long publishedLines() {
    return statsByFile.values().stream().mapToLong(s -> s.publishedLines).sum();
  }

  /**
   * 발행 실패 라인 수 총합을 반환합니다.
   *
   * @return 발행 실패 라인 수 총합
   */
  long failedLines() {
    return statsByFile.values().stream().mapToLong(s -> s.failedLines).sum();
  }
}

/**
 * 단일 파일에 대한 집계 값 객체입니다.
 *
 * <p>필드 의미:</p>
 * <ul>
 *   <li>{@link #totalLines}: 파일에서 관측된 최대 라인 번호(= 읽은 라인 수)</li>
 *   <li>{@link #skippedLines}: 스킵된 라인 수(헤더/공백 등)</li>
 *   <li>{@link #publishedLines}: publish() 호출이 성공(예외 없음)한 라인 수</li>
 *   <li>{@link #failedLines}: publish() 호출 중 예외가 발생한 라인 수</li>
 * </ul>
 *
 * @author 박성준
 * @since 1.0.0
 */
final class FileStats {

  /** 파일에서 관측된 총 라인 수(최대 라인 번호). */
  int totalLines;

  /** 헤더/공백 등 스킵된 라인 수. */
  int skippedLines;

  /** publish() 호출이 예외 없이 완료된 라인 수. */
  int publishedLines;

  /** publish() 호출 중 예외가 발생한 라인 수. */
  int failedLines;
}
