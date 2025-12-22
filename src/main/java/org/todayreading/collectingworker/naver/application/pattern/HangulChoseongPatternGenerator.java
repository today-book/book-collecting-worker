package org.todayreading.collectingworker.naver.application.pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * 한글 초성(가~하 구간)을 기반으로 쿼리 패턴을 생성하는 유틸리티 클래스입니다.
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

}
