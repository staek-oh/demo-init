package com.example.demo_260815.repository;

import com.example.demo_260815.domain.CouponIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

  boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

  // 지연 로딩으로 인한 N+1 추가 쿼리를 막기 위해 fetch join을 활용하여 연관 데이터를 즉시 가져온다.
  @Query(
      """
        select ci from CouponIssue ci
        join fetch ci.coupon
        where ci.member.id = :memberId
        order by ci.issuedAt desc
      """
  )
  List<CouponIssue> findAllByMemberIdWithCoupon(@Param("memberId") Long memberId);
}
