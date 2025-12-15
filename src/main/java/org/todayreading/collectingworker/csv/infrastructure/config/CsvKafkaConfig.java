package org.todayreading.collectingworker.csv.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * CSV 원문 라인을 Kafka로 전송하기 위한 StringSerializer 전용 Kafka 설정입니다.
 *
 * <p>전역 Kafka producer 설정은 JsonSerializer를 사용할 수 있지만,
 * CSV 라인은 따옴표가 없는 "raw 문자열" 그대로 전송해야 하므로
 * 별도의 {@link KafkaTemplate}을 정의합니다.</p>
 *
 * <p>이 설정에서 생성된 {@code csvBookKafkaTemplate} 빈은
 * value 직렬화에 {@link StringSerializer}를 사용하며,
 * CSV 관련 어댑터에서 raw 문자열 전송용으로 사용됩니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class CsvKafkaConfig {

  /** spring.kafka.* 설정을 캡슐화한 프로퍼티입니다. */
  private final KafkaProperties properties;

  /**
   * CSV 라인 전송용 StringSerializer 기반 {@link ProducerFactory}입니다.
   *
   * <p>전역 producer 설정({@link KafkaProperties#buildProducerProperties()})을 복사한 뒤,
   * key/value serializer를 {@link StringSerializer}로 덮어써
   * 문자열 기반 전송에 적합한 프로듀서 팩토리를 구성합니다.</p>
   *
   * @return CSV raw 라인 전송에 사용할 프로듀서 팩토리
   */
  @Bean
  public ProducerFactory<String, String> csvBookProducerFactory() {
    Map<String, Object> props = new HashMap<>(properties.buildProducerProperties());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  /**
   * CSV 원문 라인을 전송하기 위한 {@link KafkaTemplate}입니다.
   *
   * <p>JsonSerializer를 사용하는 기본 KafkaTemplate과는 별도로,
   * 이 템플릿은 key/value 모두 {@link StringSerializer}를 사용합니다.</p>
   *
   * @param csvBookProducerFactory CSV 전용 StringSerializer 기반 ProducerFactory
   * @return CSV raw 라인 전송용 KafkaTemplate 빈 ({@code csvBookKafkaTemplate})
   */
  @Bean
  public KafkaTemplate<String, String> csvBookKafkaTemplate(
      ProducerFactory<String, String> csvBookProducerFactory) {
    return new KafkaTemplate<>(csvBookProducerFactory);
  }
}
