package org.todayreading.collectingworker.csv.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvBookRaw {

  private String seqNo;                 // 내부 일련 번호
  private String isbnThirteenNo;        // 13자리 ISBN
  private String vlmNm;                 // 권차 정보(권 번호)
  private String titleNm;               // 도서 제목
  private String authrNm;               // 저자명
  private String publisherNm;           // 출판사명
  private String pblicteDe;             // 1차 출판일
  private String adtionSmblNm;          // 추가 기호/판차/세트 등 부가 기호
  private String prcValue;              // 도서 가격
  private String imageUrl;              // 표지 이미지
  private String bookIntrcnCn;          // 책 소개/내용 요약(소개글)
  private String kdcNm;                 // KDC 분류명(한국십진분류 코드/명칭)
  private String titleSbstNm;           // 제목 축약명/검색용 축약 제목
  private String authrSbstNm;           // 저자 축약명(검색/정렬용)
  private String twoPblicteDe;          // 2차 출판일 또는 데이터 기준일(갱신일자)
  private String intntBookstBookExstAt; // 인터넷 서점에 도서 존재 여부 플래그
  private String portalSiteBookExstAt;  // 포털 사이트에 도서 존재 여부 플래그
  private String isbnNo;                // 10자리 ISBN(구 ISBN)
}

