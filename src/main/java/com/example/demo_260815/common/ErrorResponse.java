package com.example.demo_260815.common;

public record ErrorResponse(String code, String message) {

  public static ErrorResponse from(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.name(), errorCode.getMessage());
  }
}
