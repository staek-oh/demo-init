package com.example.demo_260815.dto;

import com.example.demo_260815.domain.Reservation;
import java.time.LocalDateTime;

public record ReservationDetailResponse(Long reservationId, String memberName, String roomName, LocalDateTime reservedAt) {

  public static ReservationDetailResponse from(Reservation r) {
    return new ReservationDetailResponse(r.getId(), r.getMember().getName(), r.getRoom().getName(), r.getReserveTime());
  }
}
