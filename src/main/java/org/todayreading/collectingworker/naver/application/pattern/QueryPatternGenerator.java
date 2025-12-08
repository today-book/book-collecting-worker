package org.todayreading.collectingworker.naver.application.pattern;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

  /**
   * 일일 스캔에 사용할 쿼리 패턴을 생성합니다.
   *
   * <p>오늘 날짜의 요일을 기준으로 숫자/알파벳/한글 초성 패턴을
   * 요일별로 분배하여 일부만 반환합니다.</p>
   *
   * @return 오늘 요일에 해당하는 일일 스캔용 쿼리 문자열 목록
   */
  public static List<String> generateDailyScanQueries() {
    DayOfWeek today = LocalDate.now().getDayOfWeek();

    List<String> queries = new ArrayList<>();
    queries.addAll(AsciiPatternGenerator.dailyPatterns(today));
    queries.addAll(HangulChoseongPatternGenerator.dailyPatterns(today));

    return List.copyOf(queries);
  }
}
