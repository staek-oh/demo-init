package com.example.demo_260815.service;

import com.example.demo_260815.common.BusinessException;
import com.example.demo_260815.common.ErrorCode;
import com.example.demo_260815.domain.Coupon;
import com.example.demo_260815.domain.CouponIssue;
import com.example.demo_260815.domain.Member;
import com.example.demo_260815.dto.CouponCreateRequest;
import com.example.demo_260815.dto.CouponCreateResponse;
import com.example.demo_260815.dto.CouponIssueRequest;
import com.example.demo_260815.dto.CouponIssueResponse;
import com.example.demo_260815.dto.MemberCouponResponse;
import com.example.demo_260815.repository.CouponIssueRepository;
import com.example.demo_260815.repository.CouponRepository;
import com.example.demo_260815.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

  private final MemberRepository memberRepository;
  private final CouponRepository couponRepository;
  private final CouponIssueRepository couponIssueRepository;

  // 쿠폰 생성
  @Transactional
  public CouponCreateResponse create(CouponCreateRequest request) {
    Coupon coupon = new Coupon(request.name(), request.totalQuantity(), request.expireAt());
    couponRepository.save(coupon);
    return new CouponCreateResponse(coupon.getId());
  }

  // 쿠폰 발급
  @Transactional
  public CouponIssueResponse issue(CouponIssueRequest request) {
    // 중복 발급 검사
    if(couponIssueRepository.existsByMemberIdAndCouponId(request.memberId(), request.couponId())) {
      throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
    }

    Member member = memberRepository.findById(request.memberId())
        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

    // 락 시작
    Coupon coupon = couponRepository.findByIdForUpdate(request.couponId())
        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    LocalDateTime now = LocalDateTime.now();
    coupon.issue(now); // dirty checking

    // 발급 내역 저장
    try{
      CouponIssue couponIssue = new CouponIssue(member, coupon, now);
      couponIssueRepository.saveAndFlush(couponIssue);
      return new CouponIssueResponse(couponIssue.getId());
    } catch(DataIntegrityViolationException e) {
      throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
    }
  }

  // 멤버 쿠폰 조회
  public List<MemberCouponResponse> findMemberCoupons(Long memberId) {
    return couponIssueRepository.findAllByMemberIdWithCoupon(memberId).stream()
        .map(MemberCouponResponse::from).toList();
  }
}
