package org.todayreading.collectingworker.naver.application.pattern;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 숫자(0~9)와 영문 소문자(a~z)에 대한 쿼리 패턴을 생성하는 유틸리티 클래스입니다.
 *
 * <p>풀스캔 시에는 전체 패턴을, 일일 스캔 시에는 요일별로 분배된
 * 일부 패턴만 반환하도록 구성됩니다.
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

  /**
   * 요일별로 일일 스캔에 사용할 숫자/알파벳 패턴을 분배합니다.
   *
   * <p>{@link #fullScanPatterns()}의 인덱스를 기준으로 다음과 같이 분배합니다.
   *
   * <h3>숫자(0~9)</h3>
   * <ul>
   *   <li>월요일: '0', '1' (index 0~1)</li>
   *   <li>화요일: '2', '3' (index 2~3)</li>
   *   <li>수요일: '4', '5' (index 4~5)</li>
   *   <li>목요일: '6' (index 6)</li>
   *   <li>금요일: '7' (index 7)</li>
   *   <li>토요일: '8' (index 8)</li>
   *   <li>일요일: '9' (index 9)</li>
   * </ul>
   *
   * <h3>알파벳(a~z)</h3>
   * <ul>
   *   <li>월요일: a~d (index 10~13)</li>
   *   <li>화요일: e~h (index 14~17)</li>
   *   <li>수요일: i~l (index 18~21)</li>
   *   <li>목요일: m~p (index 22~25)</li>
   *   <li>금요일: q~t (index 26~29)</li>
   *   <li>토요일: u~w (index 30~32)</li>
   *   <li>일요일: x~z (index 33~35)</li>
   * </ul>
   *
   * @param dayOfWeek 기준 요일
   * @return 해당 요일에 사용할 숫자/알파벳 패턴 리스트
   * @author 박성준
   * @since 1.0.0
   */
  static List<String> dailyPatterns(DayOfWeek dayOfWeek) {
    List<String> ascii = fullScanPatterns();
    List<String> result = new ArrayList<>();

    // digits: index 0 ~ 9
    switch (dayOfWeek) {
      case MONDAY -> result.addAll(ascii.subList(0, 2));   // 0,1
      case TUESDAY -> result.addAll(ascii.subList(2, 4));  // 2,3
      case WEDNESDAY -> result.addAll(ascii.subList(4, 6)); // 4,5
      case THURSDAY -> result.addAll(ascii.subList(6, 7)); // 6
      case FRIDAY -> result.addAll(ascii.subList(7, 8));   // 7
      case SATURDAY -> result.addAll(ascii.subList(8, 9)); // 8
      case SUNDAY -> result.addAll(ascii.subList(9, 10));  // 9
    }

    // alphabets: index 10 ~ 35 (a~z)
    switch (dayOfWeek) {
      case MONDAY -> result.addAll(ascii.subList(10, 14)); // a,b,c,d
      case TUESDAY -> result.addAll(ascii.subList(14, 18)); // e,f,g,h
      case WEDNESDAY -> result.addAll(ascii.subList(18, 22)); // i,j,k,l
      case THURSDAY -> result.addAll(ascii.subList(22, 26)); // m,n,o,p
      case FRIDAY -> result.addAll(ascii.subList(26, 30)); // q,r,s,t
      case SATURDAY -> result.addAll(ascii.subList(30, 33)); // u,v,w
      case SUNDAY -> result.addAll(ascii.subList(33, 36)); // x,y,z
    }

    return result;
  }
}
