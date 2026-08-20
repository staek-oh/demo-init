package com.example.demo_260815.repository;

import com.example.demo_260815.domain.Reservation;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select r from Reservation r
      where r.room.id = :roomId and r.reserveDate = :reserveDate and r.reserveTime = :reserveTime
      """
  )
  boolean existsByRoomIdAndReserveDateAndReserveTime(@Param("roomId") Long roomId, @Param("reserveDate") LocalDate reserveDate, @Param("reserveTime")
      LocalDateTime reserveTime);

  @Query(
      """
      select r from Reservation r
      where r.member.id = :memberId and r.reserveDate = reserveDate
      """
  )
  List<Reservation> findByMemberIdAndReserveDate(@Param("memberId") Long memberId, LocalDate reserveDate);

  @Query(
      """
      select r from Reservation r
      join fetch r.member
      join fetch r.room
      where r.id = :id
      """
  )
  Optional<Reservation> findByIdWithMemberAndRoom(@Param("id") Long id);
}
