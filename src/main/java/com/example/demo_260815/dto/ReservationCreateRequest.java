package com.example.demo_260815.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationCreateRequest(Long memberId, Long roomId, LocalDate reserveDate, LocalDateTime reserveTime) {

}
