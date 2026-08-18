package com.example.demo_260815.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "coupon_issues",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_coupon_issues_member_coupon",
        columnNames = {"member_id", "coupon_id"}
    )
)
public class CouponIssue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id", nullable = false)
  private Coupon coupon;

  @Column(nullable = false)
  private LocalDateTime issuedAt;

  public CouponIssue(Member member, Coupon coupon, LocalDateTime issuedAt) {
    this.member = member;
    this.coupon = coupon;
    this.issuedAt = issuedAt;
  }
}
