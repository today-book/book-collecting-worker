package org.todayreading.collectingworker.naver.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Naver 책 검색 API에서 반환하는 개별 도서 정보를 표현하는 DTO이다.
 *
 * <p>도서 제목, 링크, 이미지, 저자, 가격, 출판사, 출간일, ISBN, 소개 등
 * 한 권의 도서에 대한 메타데이터를 포함한다.</p>
 *
 * <p>Naver API 응답 JSON을 그대로 매핑하는 용도로 사용하며,
 * 별도의 비즈니스 로직은 포함하지 않는다.</p>
 *
 * author 박성준
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverSearchItem(
    String title,
    String link,
    String image,
    String author,
    Integer price,
    Integer discount,
    String publisher,
    String pubdate,
    String isbn,
    String description
) {
}
