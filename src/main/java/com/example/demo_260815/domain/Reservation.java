package com.example.demo_260815.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roomr_id", nullable = false)
  private Room room;

  @Column(nullable = false)
  private LocalDate reserveDate;

  @Column(nullable = false)
  private LocalDateTime reserveTime;

  @Column(nullable = false)
  private boolean isReserved = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReserveStatus status;

  public Reservation(Member member, Room room, LocalDate reserveDate, LocalDateTime reserveTime) {
    this.member = member;
    this.room = room;
    this.reserveDate = reserveDate;
    this.reserveTime = reserveTime;
    this.status = ReserveStatus.RESERVED;
  }

  public void reserve(LocalDateTime reserveTime) {
    if(reserveTime.isAfter(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    if(this.isReserved) {
      throw new BusinessException(ErrorCode.ROOM_ALREADY_RESERVED);
    }
    isReserved = true;
  }
}
