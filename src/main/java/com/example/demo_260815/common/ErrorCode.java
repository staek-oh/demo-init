package com.example.demo_260815.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
  COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."),
  COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
  COUPON_SOLD_OUT(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다."),
  COUPON_EXPIRED(HttpStatus.CONFLICT, "만료된 쿠폰입니다."),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청이 잘못되었습니다.");

  private final HttpStatus status;
  private final String message;
}
