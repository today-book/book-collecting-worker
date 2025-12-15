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
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.todayreading.collectingworker.csv.application.dto.CsvBookRaw;

/**
 * CSV 원본 도서 데이터를 Kafka로 발행하기 위한 Kafka 설정입니다.
 *
 * <p>공통 spring.kafka.* 프로퍼티를 사용해 JsonSerializer 기반 ProducerFactory와
 * KafkaTemplate을 구성합니다.</p>
 */
@Configuration
@RequiredArgsConstructor
public class CsvKafkaConfig {

  private final KafkaProperties kafkaProperties;

  @Bean
  public ProducerFactory<String, CsvBookRaw> csvBookProducerFactory() {
    Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, CsvBookRaw> csvBookKafkaTemplate(
      ProducerFactory<String, CsvBookRaw> csvBookProducerFactory) {
    return new KafkaTemplate<>(csvBookProducerFactory);
  }
}
