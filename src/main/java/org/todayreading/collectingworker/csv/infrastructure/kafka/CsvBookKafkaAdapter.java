package org.todayreading.collectingworker.csv.infrastructure.kafka;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.csv.application.port.out.CsvBookPublishPort;
import org.todayreading.collectingworker.csv.infrastructure.config.CsvBookProperties;

/**
 * CSV 원본 라인 문자열을 Kafka로 전송하는 CSV 전용 어댑터입니다.
 *
 * <p>이 어댑터는 {@link CsvBookPublishPort}의 Kafka 기반 구현체로,
 * CSV 파일에서 읽은 한 줄(raw line)을 그대로 Kafka 토픽으로 발행합니다.
 * 컬럼 단위 파싱 및 도메인 매핑은 이 어댑터의 책임이 아니며,
 * 해당 토픽을 구독하는 컨슈머 서비스에서 처리합니다.</p>
 *
 * <p>전송에 사용할 Kafka 토픽 이름은
 * {@link CsvBookProperties}의 {@code csv.book.kafka.topic} 설정에서 주입됩니다.
 * 현재 구현은 Kafka 메시지 키를 사용하지 않고
 * {@code send(topic, value)} 형태로 전송합니다.
 * 파티셔닝 정책이나 메시지 키 기반 로직이 필요해지면 이후 확장할 수 있습니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Component
public class CsvBookKafkaAdapter implements CsvBookPublishPort {

  private static final int SAMPLE_MAX_LEN = 500;
  private static final AtomicBoolean SAMPLE_LOGGED = new AtomicBoolean(false);


  /**
   * csv.book.* 설정 값을 바인딩한 프로퍼티입니다.
   */
  private final CsvBookProperties properties;

  /**
   * CSV 원본 라인을 전송하기 위한 KafkaTemplate입니다.
   *
   * <p>설정 클래스에서 정의한 {@code csvBookKafkaTemplate} 빈이 주입되며,
   * value 직렬화에 {@code StringSerializer}를 사용합니다.</p>
   */
  private final KafkaTemplate<String, String> kafkaTemplate;

  /**
   * {@link CsvBookKafkaAdapter} 인스턴스를 생성합니다.
   *
   * @param properties    CSV 관련 설정 프로퍼티
   * @param kafkaTemplate CSV 원본 라인 전송에 사용할 KafkaTemplate
   */
  public CsvBookKafkaAdapter(
      CsvBookProperties properties,
      @Qualifier("csvBookKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate
  ) {
    this.properties = properties;
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * CSV 한 라인의 원본 문자열을 Kafka 토픽으로 전송합니다.
   *
   * <p>전송 정책:</p>
   * <ul>
   *   <li>전송할 문자열이 {@code null}이거나 공백만 포함하는 경우
   *       전송하지 않고 경고 로그를 남깁니다.</li>
   *   <li>토픽 이름은 {@link CsvBookProperties}의
   *       {@code csv.book.kafka.topic} 설정에서 조회합니다.</li>
   *   <li>정상 라인은 {@link KafkaTemplate}을 통해 비동기 전송하며,
   *       성공/실패 여부는 콜백 로그로만 기록합니다.</li>
   * </ul>
   *
   * @param rawLine CSV 원본 라인 문자열(개행 문자를 제외한 한 줄)
   */
  @Override
  public void publish(String rawLine) {
    if (rawLine == null || rawLine.isBlank()) {
      log.warn("null 또는 공백 CSV 라인은 Kafka로 전송하지 않습니다.");
      return;
    }

    // 샘플 1건만 출력(형식 확인용)
    if (SAMPLE_LOGGED.compareAndSet(false, true)) {
      String sample = abbreviate(rawLine, SAMPLE_MAX_LEN);
      log.info("CSV 샘플 1건(payload, raw line). len={}, sample={}", rawLine.length(), sample);
    }

    // application.yml의 csv.book.kafka.topic 설정에서 토픽 이름을 조회합니다.
    String topic = properties.kafka().topic();

    kafkaTemplate.send(topic, rawLine)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.warn("CSV 라인 Kafka 전송 실패.", ex);
          } else if (log.isDebugEnabled()) {
            log.debug(
                "CSV 라인 Kafka 전송 성공. topic={}, partition={}, offset={}",
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
            );
          }
        });
  }

  private static String abbreviate(String s, int maxLen) {
    if (s == null) {
      return null;
    }
    if (s.length() <= maxLen) {
      return s;
    }
    return s.substring(0, maxLen) + "...(truncated)";
  }
}
