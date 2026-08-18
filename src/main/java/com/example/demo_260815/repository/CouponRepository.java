package com.example.demo_260815.repository;

import com.example.demo_260815.domain.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

  // 쿠폰 발급 시, 중복 검사 및 재고 검사를 위한 조회와 수량 업데이트 사이에 다른 트랜잭션이 끼어들어 발생하는 값 손실을 방지하기 위해 배타락을 적용한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select c from Coupon c
      where c.id = :couponId
      """
  )
  Optional<Coupon> findByIdForUpdate(@Param("couponId") Long couponId);
}
