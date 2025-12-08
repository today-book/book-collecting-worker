package org.todayreading.collectingworker.naver.infrastructure.kafka;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;
import org.todayreading.collectingworker.naver.application.port.out.BookRawPublishPort;

/**
 * {@link BookRawPublishPort}의 Kafka 기반 구현체입니다.
 *
 * <p>수집된 네이버 도서 원시 데이터를 Kafka의 {@link #topicName} 토픽으로 발행하는
 * 인프라스트럭처 어댑터 역할을 담당합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookRawKafkaAdapter implements BookRawPublishPort {

  @Value("${naver.kafka.topic.book-raw}")
  private String topicName;

  private final KafkaTemplate<String, NaverSearchItem> kafkaTemplate;

  /**
   * 수집된 네이버 도서 아이템 목록을 Kafka 토픽으로 발행합니다.
   *
   * <p>목록이 null 이거나 비어 있는 경우에는 아무 작업도 수행하지 않습니다.
   *
   * @param items 발행할 도서 아이템 목록
   */
  @Override
  public void publish(List<NaverSearchItem> items) {
    if (items == null || items.isEmpty()) {
      log.debug("Skip publishing book.raw. items is null or empty.");
      return;
    }

    for (NaverSearchItem item : items) {
      kafkaTemplate.send(topicName, item);
    }

    log.info("Published {} items to Kafka topic '{}'.", items.size(), topicName);
  }
}
