package org.todayreading.collectingworker.csv.presentation.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.csv.application.service.CsvBookDataTransfer;
import org.todayreading.collectingworker.csv.application.service.command.CsvTransferCommand;
import org.todayreading.collectingworker.csv.infrastructure.config.CsvBookProperties;

/**
 * 애플리케이션 부팅 직후 CSV 전송 유스케이스를 자동 실행하는 Startup Runner입니다.
 *
 * <p>{@code csv.book.runner.enabled=true}일 때만 실행됩니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "csv.book.runner", name = "enabled", havingValue = "true")
public class CsvBookStartupRunner implements ApplicationRunner {

  private final CsvBookDataTransfer csvBookDataTransfer;
  private final CsvBookProperties csvBookProperties;

  @Override
  public void run(ApplicationArguments args) {
    Path inputPath = Path.of(csvBookProperties.filePath());

    if (Files.notExists(inputPath)) {
      log.warn("CSV StartupRunner 입력 경로가 존재하지 않아 실행을 건너뜁니다. inputPath={}", inputPath);
      return;
    }

    logInputSummary(inputPath);

    csvBookDataTransfer.transfer(new CsvTransferCommand(inputPath));
  }

  private void logInputSummary(Path inputPath) {
    if (Files.isDirectory(inputPath)) {
      long csvCount = countCsvFiles(inputPath);
      log.info("CSV StartupRunner 실행. inputDir={}, csvFileCount={}", inputPath, csvCount);

      if (csvCount != 18) {
        log.warn("예상 CSV 파일 수(18개)와 다릅니다. inputDir={}, csvFileCount={}", inputPath, csvCount);
      }
      return;
    }

    log.info("CSV StartupRunner 실행. inputFile={}", inputPath);
  }

  private long countCsvFiles(Path dir) {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
          .count();
    } catch (IOException e) {
      log.warn("CSV 파일 개수 집계에 실패했습니다. dir={}", dir, e);
      return -1;
    }
  }
}
