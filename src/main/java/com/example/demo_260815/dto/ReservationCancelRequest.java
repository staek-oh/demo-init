package com.example.demo_260815.dto;

import java.time.LocalDateTime;

public record ReservationCancelRequest(Long memberId, Long roomId, LocalDateTime reserveTime) {

}
