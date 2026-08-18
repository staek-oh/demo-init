package com.example.demo_260815.dto;

import com.example.demo_260815.domain.Coupon;
import com.example.demo_260815.domain.CouponIssue;
import java.time.LocalDateTime;

public record MemberCouponResponse(Long id, String name, LocalDateTime issuedAt) {

  public static MemberCouponResponse from(CouponIssue couponIssue) {
    Coupon coupon = couponIssue.getCoupon();
    return new MemberCouponResponse(coupon.getId(), coupon.getName(), coupon.getExpireAt());
  }
}
