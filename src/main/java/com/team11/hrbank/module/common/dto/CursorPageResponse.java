package com.team11.hrbank.module.common.dto;

import java.util.Base64;
import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    Long nextIdAfter,
    int size,
    long totalElements,
    boolean hasNext
) {

  /**
   * 커서 기반 페이지 응답 객체 생성
   * @param content       페이지 컨텐츠
   * @param lastId        마지막 요소의 ID
   * @param size          페이지 크기
   * @param totalElements 총 요소 수
   * @return 커서 페이지 응답
   *
   * //리턴시 사용 예시
   * return CursorPageResponse.of(dtoList, lastId, size, page.getTotalElements());
   */
  public static <T> CursorPageResponse<T> of(
          List<T> content,
          String cursorValue,
          Long lastId,
          int size,
          long totalElements,
          boolean hasNext){

    // 마지막 페이지인지
//    boolean hasNext = !content.isEmpty() && cursorValue != null;

    return new CursorPageResponse<>(
            content,
            hasNext ? cursorValue : null,
            hasNext ? lastId : null,
            size,
            totalElements,
            hasNext
    );
  }

  /**
   * 커서 문자열에서 ID 추출
   * @param cursor 커서 문자열
   * @return 추출된 ID
   *
   * 사용 예
   * 커서에서 ID 추출 (필요한 경우)
   *         if (cursor != null && !cursor.isEmpty() && idAfter == null) {
   *             idAfter = CursorPageResponse.extractIdFromCursor(cursor);
   *         }
   */
  public static Long extractIdFromCursor(String cursor) {
    if (cursor == null || cursor.isEmpty()) {
      return null;
    }

    try {
      // Base64 인코딩된 ID일 경우 디코딩 시도
      String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
      return Long.parseLong(decoded.replace("{\"id\":", "").replace("}", ""));
    } catch (Exception e) {
      // 디코딩에 실패한 경우 (프로토타입 방식의 커서인 경우), null 반환
      return null;
    }

  }

}
