package com.example.demo_260815.dto;

import com.example.demo_260815.domain.Room;

public record RoomResponse(Long id) {

  public static RoomResponse from(Room room) {
    return new RoomResponse(room.getId());
  }
}
