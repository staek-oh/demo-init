package com.example.demo_260815.service;

import com.example.demo_260815.domain.Reservation;
import com.example.demo_260815.dto.ReservationCancelRequest;
import com.example.demo_260815.dto.ReservationCreateRequest;
import com.example.demo_260815.dto.ReservationDetailResponse;
import com.example.demo_260815.dto.ReservationResponse;
import com.example.demo_260815.repository.MemberRepository;
import com.example.demo_260815.repository.ReservationRepository;
import com.example.demo_260815.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private final MemberRepository memberRepository;
  private final RoomRepository roomRepository;
  private final ReservationRepository reservationRepository;

  private final int RESERVE_LIMIT = 3;

  // 회의실 예약
  public ReservationResponse reserve(ReservationCreateRequest request) {
    Long memberId = request.memberId();
    Long roomId = request.roomId();
    LocalDate reserveDate = request.reserveDate();
    LocalDateTime reserveTime = request.reserveTime();





    // 이미 예약된 회의실인지 확인 & 배타 락 획득
    if(reservationRepository.existsByRoomIdAndReserveDateAndReserveTime(roomId, reserveDate, reserveTime)) {
      throw new BusinessException(ErrorCode.ROOM_ALREADY_RESERVED);
    }

    // 3개 초과 확인
    List<Reservation> reservationList = reservationRepository.findByMemberIdAndReserveDate(roomId, reserveTime.toLocalDate());
    if(reservationList.size() > RESERVE_LIMIT) {
      throw new BusinessException(ErrorCode.RESERVE_LIMIT_OVER);
    }



    //
  }

  // 예약 취소
  public ReservationResponse cancel(ReservationCancelRequest request) {

  }

  // 예약 현황 조회
  public ReservationDetailResponse findByid(Long reservationId) {
    Reservation r = reservationRepository.findByIdWithMemberAndRoom(reservationId).orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    return ReservationDetailResponse.from(r);
  }
}
