package org.todayreading.collectingworker.csv.application.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.todayreading.collectingworker.csv.application.dto.CsvBookRaw;
import org.todayreading.collectingworker.csv.infrastructure.kafka.CsvBookKafkaProducer;

/**
 * CSV 파일을 라인 단위로 읽어 {@link CsvBookRaw}로 매핑한 뒤 Kafka로 전송하는
 * 데이터 이관(Data Transfer) 전용 서비스입니다.
 *
 * <p>DataTransfer 스타일로 수동으로 파일 스트림을 열어 처리하며,
 * JUnit 테스트나 수동 실행 코드에서 {@link #transfer(String)} 메서드를 호출해
 * 일회성/배치성 CSV 적재 작업을 수행하는 용도로 사용합니다.
 *
 * @author 박성준
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvDataTransfer {

  private final CsvBookKafkaProducer csvKafkaProducer;

  /**
   * 지정된 경로의 CSV 파일을 읽어 각 데이터 라인을 {@link CsvBookRaw}로 매핑하고 Kafka로 전송합니다.
   *
   * <p>첫 번째 라인은 헤더로 간주해 스킵하며, 공백 라인도 건너뜁니다.
   * 각 라인의 파싱·매핑 과정에서 예외가 발생하면 해당 라인만 로그를 남기고 건너뛰며,
   * 전체 처리 흐름은 계속 진행합니다.
   *
   * @param filePath 읽을 CSV 파일의 절대 또는 상대 경로
   * @throws UncheckedIOException 파일을 여는 중 또는 읽는 중 I/O 예외가 발생한 경우
   * @author 박성준
   * @since 1.0.0
   */
  public void transfer(String filePath) {
    try (FileInputStream fis = new FileInputStream(filePath);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr, 16_000)) {

      String line;
      int lineNumber = 0;

      while ((line = br.readLine()) != null) {
        lineNumber++;

        // 1행은 헤더라고 가정하고 스킵
        if (lineNumber == 1) {
          continue;
        }
        // 완전히 빈 라인은 스킵
        if (line.isBlank()) {
          continue;
        }

        try {
          List<String> columns = pareLine(line);
          CsvBookRaw raw = mapToCsvBookRaw(columns);
          csvKafkaProducer.send(raw);
        } catch (Exception e) {
          log.warn("CSV 라인 처리 실패. lineNumber={}, line={}", lineNumber, line, e);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("CSV 파일 읽기 실패: " + filePath, e);
    }
  }

  /**
   * CSV 한 라인을 구분자 기준으로 분리해 컬럼 리스트로 변환합니다.
   *
   * <p>현재는 단순 쉼표(,) 기준 split 구현이며, 빈 컬럼도 유지하기 위해 limit 값으로 -1을 사용합니다.
   * 복잡한 따옴표/이스케이프 처리가 필요한 경우 이후 CSV 전용 라이브러리로 교체할 수 있습니다.
   *
   * @param line CSV 파일의 한 라인 문자열
   * @return 분리된 컬럼 값들의 리스트
   * @author 박성준
   * @since 1.0.0
   */
  private List<String> pareLine(String line) {
    String[] tokens = line.split(",", -1);
    return Arrays.asList(tokens);
  }

  /**
   * 파싱된 컬럼 리스트를 {@link CsvBookRaw} 인스턴스로 매핑합니다.
   *
   * <p>컬럼 순서는 다음과 같이 가정합니다:
   * <ol>
   *   <li>0: SEQ_NO</li>
   *   <li>1: ISBN_THIRTEEN_NO</li>
   *   <li>2: VLM_NM</li>
   *   <li>3: TITLE_NM</li>
   *   <li>4: AUTHR_NM</li>
   *   <li>5: PUBLISHER_NM</li>
   *   <li>6: PBLICTE_DE</li>
   *   <li>7: ADTION_SMBL_NM</li>
   *   <li>8: PRC_VALUE</li>
   *   <li>9: IMAGE_URL</li>
   *   <li>10: BOOK_INTRCN_CN</li>
   *   <li>11: KDC_NM</li>
   *   <li>12: TITLE_SBST_NM</li>
   *   <li>13: AUTHR_SBST_NM</li>
   *   <li>14: TWO_PBLICTE_DE</li>
   *   <li>15: INTNT_BOOKST_BOOK_EXST_AT</li>
   *   <li>16: PORTAL_SITE_BOOK_EXST_AT</li>
   *   <li>17: ISBN_NO</li>
   * </ol>
   *
   * @param columns CSV 한 행에서 분리한 컬럼 값 리스트
   * @return 컬럼 값을 매핑한 {@link CsvBookRaw} 인스턴스
   * @throws IllegalArgumentException 컬럼 개수가 18개 미만인 경우
   * @author 박성준
   * @since 1.0.0
   */
  private CsvBookRaw mapToCsvBookRaw(List<String> columns) {
    if (columns.size() < 18) {
      throw new IllegalArgumentException("컬럼 개수가 부족합니다. size=" + columns.size());
    }
    return CsvBookRaw.builder()
        .seqNo(columns.get(0))
        .isbnThirteenNo(columns.get(1))
        .vlmNm(columns.get(2))
        .titleNm(columns.get(3))
        .authrNm(columns.get(4))
        .publisherNm(columns.get(5))
        .pblicteDe(columns.get(6))
        .adtionSmblNm(columns.get(7))
        .prcValue(columns.get(8))
        .imageUrl(columns.get(9))
        .bookIntrcnCn(columns.get(10))
        .kdcNm(columns.get(11))
        .titleSbstNm(columns.get(12))
        .authrSbstNm(columns.get(13))
        .twoPblicteDe(columns.get(14))
        .intntBookstBookExstAt(columns.get(15))
        .portalSiteBookExstAt(columns.get(16))
        .isbnNo(columns.get(17))
        .build();
  }
}
