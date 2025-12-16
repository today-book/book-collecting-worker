package org.todayreading.collectingworker.csv.application.service;

/**
 * CSV 전송 실행 중 발생한 예외를 도메인 맥락으로 래핑합니다.
 *
 * <p>서비스 호출자가 파일 경로 등의 컨텍스트를 가진 예외를 받을 수 있도록
 * {@link CsvBookDataTransfer#transfer}에서 사용됩니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
public class CsvTransferException extends RuntimeException {

  public CsvTransferException(String message, Throwable cause) {
    super(message, cause);
  }
}
