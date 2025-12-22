package org.todayreading.collectingworker.naver.application.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 숫자(0~9)와 영문 소문자(a~z)에 대한 쿼리 패턴을 생성하는 유틸리티 클래스입니다.
 *
 * <p>이 클래스는 외부에서 직접 사용하기보다는
 * {@link QueryPatternGenerator}를 통해 사용됩니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
final class AsciiPatternGenerator {

  private AsciiPatternGenerator() {
    // 인스턴스화 방지
  }

  /**
   * 풀스캔용 숫자/알파벳 전체 패턴을 생성합니다.
   *
   * <p>반환 리스트 구조:
   * <ul>
   *   <li>index 0~9: '0' ~ '9'</li>
   *   <li>index 10~35: 'a' ~ 'z'</li>
   * </ul>
   *
   * @return 숫자 0~9와 알파벳 a~z 전체를 포함하는 문자열 리스트
   * @author 박성준
   * @since 1.0.0
   */
  static List<String> fullScanPatterns() {
    List<String> queries = new ArrayList<>();

    // '0' ~ '9'
    IntStream.rangeClosed('0', '9')
        .mapToObj(c -> String.valueOf((char) c))
        .forEach(queries::add);

    // 'a' ~ 'z'
    IntStream.rangeClosed('a', 'z')
        .mapToObj(c -> String.valueOf((char) c))
        .forEach(queries::add);

    return queries;
  }

}
