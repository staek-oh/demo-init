package com.example.demo_260815.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record CouponCreateRequest(String name, LocalDateTime expireAt, int totalQuantity) {

}
