package org.todayreading.collectingworker.naver.application.pattern;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * 한글 초성(가~하 구간)을 기반으로 쿼리 패턴을 생성하는 유틸리티 클래스입니다.
 *
 * <p>풀스캔 시에는 전체 초성 패턴을, 일일 스캔 시에는 요일별로 분배된
 * 일부 초성 패턴만 반환하도록 구성됩니다.
 *
 * <p>이 클래스는 외부에서 직접 사용하기보다는
 * {@link QueryPatternGenerator}를 통해 사용됩니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
final class HangulChoseongPatternGenerator {

  private HangulChoseongPatternGenerator() {
    // 인스턴스화 방지
  }

  /**
   * 풀스캔용 한글 초성 전체 패턴을 생성합니다.
   *
   * <p>유니코드 한글 완성형(AC00)을 기준으로,
   * 각 초성의 첫 번째 음절(가, 까, 나, 다, ...)을 계산하여 반환합니다.
   *
   * @return 한글 초성 전체(19개)를 포함하는 문자열 리스트
   * @author 박성준
   * @since 1.0.0
   */
  static List<String> fullScanPatterns() {
    List<String> queries = new ArrayList<>();

    final int HANGUL_BASE = 0xAC00; // '가'
    final int CHOSEONG_COUNT = 19;  // 초성 개수
    final int VOWEL_COUNT = 21;     // 중성 개수
    final int JONGSEONG_COUNT = 28; // 종성 개수
    final int BLOCK_SIZE = VOWEL_COUNT * JONGSEONG_COUNT; // 588

    for (int i = 0; i < CHOSEONG_COUNT; i++) {
      char syllable = (char) (HANGUL_BASE + (i * BLOCK_SIZE));
      queries.add(String.valueOf(syllable));
    }

    return queries;
  }

  /**
   * 요일별로 일일 스캔에 사용할 한글 초성 패턴을 분배합니다.
   *
   * <p>{@link #fullScanPatterns()}의 인덱스를 기준으로 다음과 같이 분배합니다
   * (총 19개 초성):
   *
   * <ul>
   *   <li>월요일: index 0~2 (3개)</li>
   *   <li>화요일: index 3~5 (3개)</li>
   *   <li>수요일: index 6~8 (3개)</li>
   *   <li>목요일: index 9~11 (3개)</li>
   *   <li>금요일: index 12~14 (3개)</li>
   *   <li>토요일: index 15~16 (2개)</li>
   *   <li>일요일: index 17~18 (2개)</li>
   * </ul>
   *
   * @param dayOfWeek 기준 요일
   * @return 해당 요일에 사용할 한글 초성 패턴 리스트
   * @author 박성준
   * @since 1.0.0
   */
  static List<String> dailyPatterns(DayOfWeek dayOfWeek) {
    List<String> hangul = fullScanPatterns();
    List<String> result = new ArrayList<>();

    // hangul: index 0 ~ 18 (19개)
    switch (dayOfWeek) {
      case MONDAY -> result.addAll(subListSafe(hangul, 0, 3));
      case TUESDAY -> result.addAll(subListSafe(hangul, 3, 6));
      case WEDNESDAY -> result.addAll(subListSafe(hangul, 6, 9));
      case THURSDAY -> result.addAll(subListSafe(hangul, 9, 12));
      case FRIDAY -> result.addAll(subListSafe(hangul, 12, 15));
      case SATURDAY -> result.addAll(subListSafe(hangul, 15, 17));
      case SUNDAY -> result.addAll(subListSafe(hangul, 17, 19));
    }

    return result;
  }

  /**
   * subList 요청 구간이 리스트 크기를 넘어가더라도
   * IndexOutOfBoundsException이 발생하지 않도록 방어하는 헬퍼 메서드입니다.
   * IndexOutOfBoundsException이란?
   * from < 0 이거나
   * to > size 이거나
   * from > to 이면 전부 예외가 나는 exception
   *
   * @param source 원본 리스트
   * @param fromIndex 시작 인덱스(포함)
   * @param toIndex 끝 인덱스(배타)
   * @return 유효 범위 내에서 잘려진 subList 결과
   */
  private static List<String> subListSafe(List<String> source, int fromIndex, int toIndex) {
    int size = source.size();
    if (fromIndex >= size) {
      return List.of();
    }
    int safeToIndex = Math.min(toIndex, size);
    if (fromIndex >= safeToIndex) {
      return List.of();
    }
    return new ArrayList<>(source.subList(fromIndex, safeToIndex));
  }
}
