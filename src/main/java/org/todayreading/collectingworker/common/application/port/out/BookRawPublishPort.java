package org.todayreading.collectingworker.common.application.port.out;

import java.util.List;
import org.todayreading.collectingworker.naver.application.dto.NaverSearchItem;

/**
 * 수집된 네이버 도서 원시 데이터를 외부 시스템(Kafka, RabbitMQ 등)으로
 * 발행하기 위한 출력 포트 인터페이스입니다.
 *
 * <p>구현체는 인프라스트럭처 레이어에서 메시지 브로커별로 제공하며,
 * 애플리케이션 레이어에서는 이 포트만 의존함으로써
 * 특정 브로커(Kafka, RabbitMQ 등)에 대한 의존을 회피합니다.
 *
 * <p>초기에는 {@link NaverSearchItem}을 그대로 전송 대상으로 사용하지만,
 * 추후 도메인 모델이 정리되면 별도의 도메인 객체로 교체할 수 있습니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
public interface BookRawPublishPort {

  /**
   * 수집된 네이버 도서 아이템 목록을 외부 시스템으로 발행합니다.
   *
   * @param items 발행할 도서 아이템 목록 (비어있을 수 있음)
   */
  void publish(List<NaverSearchItem> items);
}
