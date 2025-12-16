package org.todayreading.collectingworker.csv.application.service.command;

import java.nio.file.Path;
import java.util.Objects;

/**
 * CSV 전송 유스케이스의 입력(Command) 객체입니다.
 *
 * <p>이 커맨드는 CSV 전송 실행에 필요한 입력 값을 캡슐화합니다.
 * 현재는 입력 경로만 포함하지만, 향후 실행 옵션(예: 헤더 스킵 라인 수, 재귀 탐색, 파일 패턴,
 * 전송 모드, 라인 범위 등)을 확장해도 유스케이스 메서드 시그니처를 변경하지 않도록 설계되었습니다.</p>
 *
 * @param inputPath CSV 파일 또는 CSV 파일들이 있는 디렉터리 경로
 * @author 박성준
 * @since 1.0.0
 */
public record CsvTransferCommand(Path inputPath) {

  /**
   * 커맨드 생성 시 필수 입력을 검증합니다.
   *
   * @param inputPath CSV 입력 경로
   */
  public CsvTransferCommand {
    Objects.requireNonNull(inputPath, "inputPath must not be null");
  }

  /**
   * 문자열 경로로부터 커맨드를 생성하는 팩토리 메서드입니다.
   *
   * <p>외부(컨트롤러/러너/스케줄러 등)에서 문자열 설정 값을 받을 때
   * {@link Path#of(String, String...)} 변환을 한 번에 처리할 수 있습니다.</p>
   *
   * @param inputPath CSV 입력 경로 문자열
   * @return {@link CsvTransferCommand}
   * @throws NullPointerException inputPath가 null인 경우
   * @throws IllegalArgumentException inputPath가 blank인 경우
   */
  public static CsvTransferCommand of(String inputPath) {
    Objects.requireNonNull(inputPath, "inputPath must not be null");
    if (inputPath.isBlank()) {
      throw new IllegalArgumentException("inputPath must not be blank");
    }
    return new CsvTransferCommand(Path.of(inputPath));
  }
}
