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

  /**
   * 숫자(0~9)에 대한 요일별 인덱스 범위 테이블입니다.
   *
   * <p>{@link #fullScanPatterns()} 기준 인덱스:
   * <ul>
   *   <li>0~9: '0' ~ '9'</li>
   * </ul>
   *
   * <p>dayIndex = {@link DayOfWeek#getValue()} - 1 (월=0, ..., 일=6) 을 사용합니다.</p>
   *
   * <ul>
   *   <li>월(0): [0, 2)  → '0','1'</li>
   *   <li>화(1): [2, 4)  → '2','3'</li>
   *   <li>수(2): [4, 6)  → '4','5'</li>
   *   <li>목(3): [6, 7)  → '6'</li>
   *   <li>금(4): [7, 8)  → '7'</li>
   *   <li>토(5): [8, 9)  → '8'</li>
   *   <li>일(6): [9,10) → '9'</li>
   * </ul>
   */
  private static final int[][] DIGIT_RANGES = {
      {0, 2},  // MONDAY
      {2, 4},  // TUESDAY
      {4, 6},  // WEDNESDAY
      {6, 7},  // THURSDAY
      {7, 8},  // FRIDAY
      {8, 9},  // SATURDAY
      {9, 10}  // SUNDAY
  };

  /**
   * 알파벳(a~z)에 대한 요일별 인덱스 범위 테이블입니다.
   *
   * <p>{@link #fullScanPatterns()} 기준 인덱스:
   * <ul>
   *   <li>10~35: 'a' ~ 'z'</li>
   * </ul>
   *
   * <ul>
   *   <li>월(0): [10,14) → a,b,c,d</li>
   *   <li>화(1): [14,18) → e,f,g,h</li>
   *   <li>수(2): [18,22) → i,j,k,l</li>
   *   <li>목(3): [22,26) → m,n,o,p</li>
   *   <li>금(4): [26,30) → q,r,s,t</li>
   *   <li>토(5): [30,33) → u,v,w</li>
   *   <li>일(6): [33,36) → x,y,z</li>
   * </ul>
   */
  private static final int[][] ALPHA_RANGES = {
      {10, 14},  // MONDAY
      {14, 18},  // TUESDAY
      {18, 22},  // WEDNESDAY
      {22, 26},  // THURSDAY
      {26, 30},  // FRIDAY
      {30, 33},  // SATURDAY
      {33, 36}   // SUNDAY
  };

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
   * <p>{@link #fullScanPatterns()}의 인덱스를 기준으로
   * {@link #DIGIT_RANGES}, {@link #ALPHA_RANGES} 테이블에서
   * 오늘 요일에 해당하는 구간만 선택하여 반환합니다.</p>
   *
   * @param dayOfWeek 기준 요일
   * @return 해당 요일에 사용할 숫자/알파벳 패턴 리스트
   * @author 박성준
   * @since 1.0.0
   */
  static List<String> dailyPatterns(DayOfWeek dayOfWeek) {
    List<String> ascii = fullScanPatterns();

    int dayIndex = dayOfWeek.getValue() - 1; // MONDAY=1 → 0, ..., SUNDAY=7 → 6

    int[] digitRange = DIGIT_RANGES[dayIndex];
    List<String> result = new ArrayList<>(ascii.subList(digitRange[0], digitRange[1]));

    int[] alphaRange = ALPHA_RANGES[dayIndex];
    result.addAll(ascii.subList(alphaRange[0], alphaRange[1]));

    return result;
  }
}
