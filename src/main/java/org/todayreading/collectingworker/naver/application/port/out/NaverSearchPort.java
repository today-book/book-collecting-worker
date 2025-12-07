package org.todayreading.collectingworker.naver.application.port.out;


import org.todayreading.collectingworker.naver.application.dto.NaverSearchResponse;

public interface NaverSearchPort {

  NaverSearchResponse search(String query,
      Integer display,
      Integer start,
      String sort);
}
