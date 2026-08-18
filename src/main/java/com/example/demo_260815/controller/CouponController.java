package com.example.demo_260815.controller;

import com.example.demo_260815.dto.CouponCreateRequest;
import com.example.demo_260815.dto.CouponCreateResponse;
import com.example.demo_260815.dto.CouponIssueRequest;
import com.example.demo_260815.dto.CouponIssueResponse;
import com.example.demo_260815.dto.MemberCouponResponse;
import com.example.demo_260815.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

  private final CouponService couponService;

  @GetMapping("/members/{memberId}/coupons")
  public List<MemberCouponResponse> findMemberCoupons(@PathVariable Long memberId) {
    return couponService.findMemberCoupons(memberId);
  }

  @PostMapping("/coupons")
  public CouponCreateResponse create(@RequestBody @Valid CouponCreateRequest request) {
    return couponService.create(request);
  }

  @PostMapping("/coupons/{couponId}/issue")
  public CouponIssueResponse issue(@RequestBody @Valid CouponIssueRequest request, @PathVariable Long couponId) {
    return couponService.issue(request);
  }
}
