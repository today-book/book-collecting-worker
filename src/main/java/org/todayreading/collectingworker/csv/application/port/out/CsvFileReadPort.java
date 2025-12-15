package org.todayreading.collectingworker.csv.application.port.out;

import java.nio.file.Path;
import java.util.Objects;

/**
 * CSV 입력(파일 또는 디렉터리)을 읽어 파일 단위 이벤트로 전달하는 출력 포트입니다.
 *
 * <p>애플리케이션 계층은 파일 시스템 구현(Files/BufferedReader 등)에 직접 의존하지 않고,
 * 이 포트를 통해 "파일 경계"가 명확한 이벤트 스트림을 공급받습니다.</p>
 *
 * <p>이 포트의 핵심 계약:</p>
 * <ul>
 *   <li>구현체는 처리 대상 파일마다 {@link CsvFileReadListener#onFileStart(Path)}를 먼저 호출해야 합니다.</li>
 *   <li>파일의 각 라인을 읽을 때마다 {@link CsvFileReadListener#onLine(Path, int, String)}을 호출해야 합니다.</li>
 *   <li>파일 처리가 끝나면(0라인 파일 포함) {@link CsvFileReadListener#onFileEnd(Path)}를 반드시 호출해야 합니다.</li>
 * </ul>
 *
 * <p>라인 번호는 파일 내에서 1부터 시작해야 하며, 파일이 바뀌면 다시 1부터 시작해야 합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
public interface CsvFileReadPort {

  /**
   * 주어진 경로(파일 또는 디렉터리)의 CSV 파일들을 읽어, 파일 단위 이벤트로 전달합니다.
   *
   * @param inputPath CSV 파일 또는 CSV 파일들이 있는 디렉터리 경로
   * @param listener  파일 단위 이벤트 리스너
   */
  void read(Path inputPath, CsvFileReadListener listener);

  /**
   * CSV 파일 읽기 이벤트 리스너입니다.
   *
   * <p>구현체는 파일 경계를 명확히 전달하기 위해 {@code onFileStart -> onLine* -> onFileEnd} 순서를 보장해야 합니다.</p>
   */
  interface CsvFileReadListener {

    /**
     * 파일 읽기를 시작할 때 호출됩니다.
     *
     * <p>빈 파일(0라인)이라도 반드시 호출되어야 합니다.</p>
     *
     * @param filePath 현재 읽기 시작한 파일 경로
     */
    void onFileStart(Path filePath);

    /**
     * 파일에서 한 라인을 읽을 때마다 호출됩니다.
     *
     * @param filePath   현재 읽고 있는 파일 경로
     * @param lineNumber 현재 파일 내 라인 번호(1부터 시작)
     * @param line       CSV 원본 라인 문자열(개행 제외)
     */
    void onLine(Path filePath, int lineNumber, String line);

    /**
     * 파일 읽기가 종료될 때 호출됩니다.
     *
     * <p>빈 파일(0라인)이라도 {@link #onFileStart(Path)} 이후 반드시 호출되어야 합니다.</p>
     *
     * @param filePath 현재 읽기를 종료한 파일 경로
     */
    void onFileEnd(Path filePath);
  }

  /**
   * {@link CsvFileReadListener}의 빈(no-op) 구현을 제공하는 어댑터 클래스입니다.
   *
   * <p>필요한 이벤트만 선택적으로 오버라이드할 수 있도록 돕습니다.</p>
   */
  abstract class CsvFileReadListenerAdapter implements CsvFileReadListener {

    @Override
    public void onFileStart(Path filePath) {
      Objects.requireNonNull(filePath, "filePath must not be null");
    }

    @Override
    public void onLine(Path filePath, int lineNumber, String line) {
      Objects.requireNonNull(filePath, "filePath must not be null");
    }

    @Override
    public void onFileEnd(Path filePath) {
      Objects.requireNonNull(filePath, "filePath must not be null");
    }
  }
}
