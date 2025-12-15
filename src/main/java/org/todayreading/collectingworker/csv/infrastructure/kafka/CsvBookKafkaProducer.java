package org.todayreading.collectingworker.csv.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.csv.application.dto.CsvBookRaw;

/**
 * CSV로 적재된 도서 원본 데이터를 Kafka로 전송하는 프로듀서입니다.
 *
 * <p>{@link CsvBookRaw}를 JSON 형태로 직렬화해 {@link #topicName} 토픽으로 발행합니다.
 * 전송할 데이터가 null 이면 로깅 후 무시합니다.</p>
 */
@Slf4j
@Component
public class CsvBookKafkaProducer {

  @Value("${csv.kafka.topic:${naver.kafka.topic}}")
  private String topicName;

  private final KafkaTemplate<String, CsvBookRaw> kafkaTemplate;

  @Autowired
  public CsvBookKafkaProducer(
      @Qualifier("csvBookKafkaTemplate")
      KafkaTemplate<String, CsvBookRaw> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * CSV 원본 도서 데이터를 Kafka 토픽으로 발행합니다.
   *
   * @param bookRaw 전송할 CSV 도서 원본 데이터
   */
  public void send(CsvBookRaw bookRaw) {
    if (bookRaw == null) {
      log.debug("Skip sending csv book raw. bookRaw is null.");
      return;
    }
    kafkaTemplate.send(topicName, bookRaw);
  }
}
