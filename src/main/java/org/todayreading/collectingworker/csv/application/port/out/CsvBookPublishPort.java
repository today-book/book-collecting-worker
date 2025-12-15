package org.todayreading.collectingworker.csv.application.port.out;

/**
 * CSV 원본 데이터를 외부 시스템(Kafka 등)으로 발행하기 위한 출력 포트입니다.
 *
 * <p>이 포트는 CSV 한 라인의 <b>원본 문자열</b>을 그대로 전달받아
 * 메시지 브로커 등으로 전송하는 역할만 정의합니다.
 * 실제 전송 방식(Kafka, 파일, 기타)은 인프라스트럭처 어댑터에서 구현합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
public interface CsvBookPublishPort {

  /**
   * CSV 한 라인의 원본 문자열을 외부 시스템으로 발행합니다.
   *
   * @param rawLine CSV 원본 라인 문자열(헤더/빈 라인은 제외된 상태여야 함)
   */
  void publish(String rawLine);
}
