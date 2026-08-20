package com.example.demo_260815.dto;

import com.example.demo_260815.domain.Reservation;

public record ReservationResponse(Long id) {

  public static ReservationResponse from(Reservation r) {
    return new ReservationResponse(r.getId());
  }
}
