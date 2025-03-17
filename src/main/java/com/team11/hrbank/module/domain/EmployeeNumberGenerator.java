package com.team11.hrbank.module.domain;

import java.security.SecureRandom;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeNumberGenerator {

  private final SecureRandom secureRandom = new SecureRandom();

  public String generateEmployeeNumber() {
    int year = LocalDate.now().getYear();
    StringBuilder stringBuilder = new StringBuilder();

    /**
     * 추신:
     * min 100_000_000_000 ~ max 999_999_999_999
     * min + (long)(secureRandom.nexDouble() * (max-min)) 해도 되지만,
     *
     * 당장은 아래가 직관적일 것 같아 밑과 같이 작성했습니다.
     *
     * 문구는 모두가 어떤 방식으로든 확인 완료하면 삭제합니다.
     * 더 좋은 방법이 있다면 공유해주세요.
     **/

    for (int i = 0; i < 12; i++) {
      stringBuilder.append(secureRandom.nextInt(10));
    }

    return String.format("EMP-%d-73%s", year, stringBuilder);

  }
}
