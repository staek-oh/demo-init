package com.example.demo_260815.domain;

import com.example.demo_260815.common.BusinessException;
import com.example.demo_260815.common.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)

public class Coupon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private int totalQuantity;

  @Column(nullable = false)
  private int issuedQuantity = 0;

  @Column(nullable = false)
  private LocalDateTime expireAt;

  public Coupon(String name, int totalQuantity, LocalDateTime expireAt) {
    this.name = name;
    this.totalQuantity = totalQuantity;
    this.expireAt = expireAt;
  }

  public void issue(LocalDateTime now) {
    if(now.isAfter(this.expireAt)) {
      throw new BusinessException(ErrorCode.COUPON_EXPIRED);
    }
    if(issuedQuantity >= totalQuantity) {
      throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
    }
    issuedQuantity++;
  }
}
