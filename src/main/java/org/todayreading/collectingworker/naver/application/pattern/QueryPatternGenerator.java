package org.todayreading.collectingworker.naver.application.pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * 네이버 도서 수집에 사용할 검색어 패턴을 생성하는 파사드 유틸리티입니다.
 *
 * <p>숫자/알파벳 패턴은 {@link AsciiPatternGenerator},
 * 한글 초성 패턴은 {@link HangulChoseongPatternGenerator}에 위임합니다.</p>
 *
 * @author 박성준
 * @since 1.0.0
 */
public final class QueryPatternGenerator {

  private QueryPatternGenerator() {
    // 인스턴스화 방지
  }

  /**
   * 초기/풀스캔용 전체 쿼리 패턴을 생성합니다.
   *
   * <p>숫자 0~9, 알파벳 a~z, 한글 초성(가~하)을 모두 포함합니다.</p>
   *
   * @return 풀스캔에 사용할 쿼리 문자열 목록
   */
  public static List<String> generateFullScanQueries() {
    List<String> queries = new ArrayList<>();
    queries.addAll(AsciiPatternGenerator.fullScanPatterns());
    queries.addAll(HangulChoseongPatternGenerator.fullScanPatterns());
    return List.copyOf(queries);
  }

}
