package org.todayreading.collectingworker.csv.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CSV 도서 수집에 필요한 설정 값을 바인딩하는 프로퍼티 레코드입니다.
 *
 * <p>application.yml의 {@code csv.book.*} 설정을 매핑하며,
 * Bean Validation을 통해 필수 값에 대한 검증을 수행합니다.</p>
 *
 * <pre>
 * csv:
 *   book:
 *     file-path: ${CSV_BOOK_FILE_PATH}
 *     kafka:
 *       topic: csv-book.raw
 * </pre>
 *
 * 위와 같은 설정을 기준으로 다음 컴포넌트에 매핑됩니다.
 * <ul>
 *   <li>{@link #filePath()} 는 {@code csv.book.file-path} 설정을 매핑합니다.</li>
 *   <li>{@link #kafka()}의 {@link KafkaProperties#topic() topic()} 는
 *       {@code csv.book.kafka.topic} 설정을 매핑합니다.</li>
 * </ul>
 *
 * <p>이 레코드는 불변(immutable) 설정 객체로 사용되며,
 * 값 변경은 application.yml(또는 환경 변수, 시스템 프로퍼티 등 외부 설정)에서만 이루어집니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "csv.book")
public record CsvBookProperties(

    /*
     * 처리할 CSV 파일의 절대/상대 경로입니다.
     *
     * <p>예:</p>
     * <ul>
     *   <li>로컬: {@code /Users/seongjun/Documents/thirdproject/data/csv/xxx.csv}</li>
     *   <li>EC2: {@code /data/csv/xxx.csv}</li>
     * </ul>
     */
    @NotBlank
    String filePath,

    /*
     * CSV Book 전용 Kafka 설정입니다.
     *
     * <p>{@code csv.book.kafka.*} 하위 설정을 매핑합니다.</p>
     */
    KafkaProperties kafka
) {

  /**
   * CSV Book 전용 Kafka 설정을 담는 중첩 프로퍼티 레코드입니다.
   *
   * <p>application.yml의 {@code csv.book.kafka.*}에 매핑됩니다.</p>
   */
  public record KafkaProperties(

      /*
       * CSV 원본 라인(raw line)을 전송할 Kafka 토픽명입니다.
       *
       * <p>예: {@code csv-book.raw}</p>
       */
      @NotBlank
      String topic
  ) {
  }
}
