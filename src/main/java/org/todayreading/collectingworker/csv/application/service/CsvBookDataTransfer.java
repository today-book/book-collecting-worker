package org.todayreading.collectingworker.csv.application.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.csv.application.port.out.CsvBookPublishPort;

/**
 * CSV 파일을 라인 단위로 읽어, 각 라인의 원본 문자열을
 * {@link CsvBookPublishPort}를 통해 외부 시스템(Kafka 등)으로 발행하는
 * 애플리케이션 서비스입니다.
 *
 * <p>이 클래스는 CSV 파일을 읽어 <b>파싱하지 않은 원본 라인(raw line)</b> 그대로
 * 메시지 브로커로 전달하는 역할만 담당하며,
 * 컬럼 단위 파싱 및 도메인 매핑은 이후 단계(컨슈머 서비스)에서 수행합니다.
 *
 * <p>이 클래스의 책임:
 * <ul>
 *   <li>설정 파일(csv.book.file-path)에 지정된 CSV 파일 경로를 사용해 파일 스트림을 연다</li>
 *   <li>파일을 라인 단위로 안전하게 읽는다</li>
 *   <li>헤더 라인 및 공백 라인은 스킵한다</li>
 *   <li>각 라인을 {@link CsvBookPublishPort}를 통해 발행한다</li>
 *   <li>라인 처리 중 예외 발생 시 해당 라인만 로그로 남기고 다음 라인으로 계속 진행한다</li>
 * </ul>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvBookDataTransfer {

  /** CSV 파일을 라인 단위로 읽을 때 사용할 버퍼 크기(문자 단위)입니다. */
  private static final int CSV_BUFFER_SIZE = 16_000;

  private final CsvBookPublishPort csvBookPublishPort;

  /** application.yml 의 {@code csv.book.file-path}에서 주입되는 CSV 파일 경로입니다. */
  @Value("${csv.book.file-path}")
  private String csvBookFilePath;

  /**
   * 설정된 CSV 파일 경로( {@code csv.book.file-path} )의 CSV 파일을 읽어,
   * 각 라인의 원본 문자열을 {@link CsvBookPublishPort}를 통해 발행합니다.
   *
   * <p>처리 정책:
   * <ul>
   *   <li>첫 번째 라인은 헤더로 간주하고 스킵합니다.</li>
   *   <li>빈 라인(공백 문자열)만 포함된 라인은 스킵합니다.</li>
   *   <li>각 라인 처리 중 예외가 발생하면 해당 라인만 로그로 남기고 다음 라인으로 계속 진행합니다.</li>
   *   <li>파일 오픈/읽기 중 발생한 {@link IOException}은 {@link UncheckedIOException}으로 래핑하여 전파합니다.</li>
   * </ul>
   *
   * @throws UncheckedIOException 파일 읽기 실패 시 발생
   */
  public void transfer() {
    if (csvBookFilePath == null || csvBookFilePath.isBlank()) {
      throw new IllegalStateException("csv.book.file-path 설정이 비어 있습니다.");
    }

    log.info("CSV 파일 전송 시작. filePath={}", csvBookFilePath);

    try (FileInputStream fis = new FileInputStream(csvBookFilePath);
        InputStreamReader isr = new InputStreamReader(fis, UTF_8);
        BufferedReader br = new BufferedReader(isr, CSV_BUFFER_SIZE)) {

      String line;
      int lineNumber = 0;

      while ((line = br.readLine()) != null) {
        lineNumber++;

        if (shouldSkipLine(lineNumber, line)) {
          continue;
        }

        processLine(lineNumber, line);
      }

      log.info("CSV 파일 전송 완료. filePath={}, totalLines={}", csvBookFilePath, lineNumber);
    } catch (IOException e) {
      throw new UncheckedIOException("CSV 파일 읽기 실패: " + csvBookFilePath, e);
    }
  }

  /**
   * 1행(헤더) 또는 빈 라인 여부를 판단합니다.
   *
   * <p>이 메서드는 {@link BufferedReader#readLine()} 호출 이후에만 사용되므로
   * {@code line} 파라미터가 {@code null}이 아님이 보장됩니다.
   *
   * @param lineNumber 현재 라인 번호(1부터 시작)
   * @param line 읽은 라인 문자열
   * @return 스킵해야 하는 라인인 경우 {@code true}, 그렇지 않으면 {@code false}
   */
  private static boolean shouldSkipLine(int lineNumber, String line) {
    if (lineNumber == 1) {
      // 헤더 라인 스킵
      return true;
    }
    if (line.isBlank()) {
      // 공백 라인 스킵
      return true;
    }
    return false;
  }

  /**
   * 단일 CSV 라인(원본 문자열)을 {@link CsvBookPublishPort}를 통해 외부 시스템으로 발행합니다.
   *
   * <p>이 과정에서 발생하는 모든 예외는 잡아서 경고 로그를 남기고,
   * 호출자(루프)는 다음 라인 처리를 계속 진행할 수 있도록 합니다.
   *
   * @param lineNumber 현재 처리 중인 라인 번호
   * @param line CSV 원본 라인 문자열
   */
  private void processLine(int lineNumber, String line) {
    try {
      csvBookPublishPort.publish(line);
    } catch (Exception e) {
      log.warn("CSV 라인 발행 실패. lineNumber={}", lineNumber, e);
    }
  }
}
