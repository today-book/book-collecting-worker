package org.todayreading.collectingworker.csv.infrastructure.fs;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.csv.application.port.out.CsvFileReadPort;

/**
 * 로컬(파일시스템)에서 CSV 파일(또는 디렉터리)을 읽어 파일 단위 이벤트로 전달하는 어댑터입니다.
 *
 * <p>{@link CsvFileReadPort}의 구현체로서, 입력 경로가</p>
 * <ul>
 *   <li>파일이면: 해당 파일 1개를 읽어 {@code onFileStart -> onLine* -> onFileEnd} 호출</li>
 *   <li>디렉터리면: 디렉터리 내 {@code *.csv} 파일을 정렬 후 순회하며 위 이벤트 시퀀스를 반복</li>
 * </ul>
 *
 * <p>빈 파일(0라인)이라도 {@code onFileStart}와 {@code onFileEnd}는 반드시 호출합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Component
public class CsvLocalReader implements CsvFileReadPort {

  /** 파일을 라인 단위로 읽을 때 사용할 버퍼 크기(문자 단위)입니다. */
  private static final int DEFAULT_BUFFER_SIZE = 16_000;

  /**
   * 입력 경로(파일/디렉터리)를 읽어 파일 단위 이벤트로 전달합니다.
   *
   * <p>이 메서드는 "분기(라우팅)" 역할을 합니다.</p>
   * <ul>
   *   <li>디렉터리면: {@link #listCsvFiles(Path)}로 대상 파일 목록을 만든 뒤 파일별로 읽습니다.</li>
   *   <li>파일이면: 해당 파일 1개를 읽습니다.</li>
   * </ul>
   *
   * @param inputPath CSV 파일 또는 CSV 파일들이 있는 디렉터리 경로
   * @param listener 파일 단위 이벤트 리스너
   */
  @Override
  public void read(Path inputPath, CsvFileReadListener listener) {
    Objects.requireNonNull(inputPath, "inputPath must not be null");
    Objects.requireNonNull(listener, "listener must not be null");

    if (Files.notExists(inputPath)) {
      throw new IllegalStateException("CSV 입력 경로가 존재하지 않습니다. inputPath=" + inputPath);
    }

    if (Files.isDirectory(inputPath)) {
      readDirectory(inputPath, listener);
      return;
    }

    readSingleFile(inputPath, listener);
  }

  /**
   * 디렉터리 내 {@code *.csv} 파일을 정렬된 순서로 읽어 이벤트로 전달합니다.
   *
   * @param dir 디렉터리 경로
   * @param listener 파일 단위 이벤트 리스너
   */
  private void readDirectory(Path dir, CsvFileReadListener listener) {
    List<Path> csvFiles = listCsvFiles(dir);

    if (csvFiles.isEmpty()) {
      log.warn("CSV 디렉터리에 .csv 파일이 없습니다. dir={}", dir);
      return;
    }

    for (Path file : csvFiles) {
      readSingleFile(file, listener);
    }
  }

  /**
   * 디렉터리에서 {@code *.csv} 파일 목록을 조회하여 파일명 오름차순으로 정렬해 반환합니다.
   *
   * <p>현재 구현은 디렉터리 1-depth만 조회합니다(재귀 X).</p>
   *
   * @param dir CSV 파일들이 들어있는 디렉터리
   * @return 정렬된 CSV 파일 목록
   */
  private List<Path> listCsvFiles(Path dir) {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("CSV 디렉터리 목록 조회 실패: " + dir, e);
    }
  }

  /**
   * 단일 파일을 UTF-8로 라인 단위 읽어 {@code onFileStart -> onLine* -> onFileEnd}를 호출합니다.
   *
   * <p>빈 파일(0라인)이라도 {@code onFileStart}와 {@code onFileEnd}는 반드시 호출합니다.</p>
   *
   * @param file 읽을 파일 경로
   * @param listener 파일 단위 이벤트 리스너
   */
  private void readSingleFile(Path file, CsvFileReadListener listener) {
    validateRegularFile(file);

    listener.onFileStart(file);

    try (BufferedReader br =
        new BufferedReader(Files.newBufferedReader(file, UTF_8), DEFAULT_BUFFER_SIZE)) {

      String line;
      int lineNumber = 0;

      while ((line = br.readLine()) != null) {
        lineNumber++;
        listener.onLine(file, lineNumber, line);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("CSV 파일 읽기 실패: " + file, e);
    } finally {
      // IO 예외가 발생하더라도 "파일 경계 이벤트"를 가능한 한 보장하기 위해 finally에서 종료 이벤트를 호출합니다.
      listener.onFileEnd(file);
    }
  }

  /**
   * 입력 경로가 "존재하는 일반 파일"인지 검증합니다.
   *
   * @param file 파일 경로
   */
  private void validateRegularFile(Path file) {
    if (Files.notExists(file)) {
      throw new IllegalStateException("CSV 파일이 존재하지 않습니다. filePath=" + file);
    }
    if (!Files.isRegularFile(file)) {
      throw new IllegalStateException("CSV 입력 경로가 파일이 아닙니다. filePath=" + file);
    }
  }
}
