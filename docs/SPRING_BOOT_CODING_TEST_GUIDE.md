# Spring Boot 과제형 코딩테스트 대비 가이드

> 대상: `demo-init` 프로젝트 (Spring Boot 4.1.0, Java 21, Gradle, H2)
> 목표: 하루 6시간 × 4~5일, 70분 타임어택형 과제 테스트 대비
> 전제: DB는 H2 인메모리로 통일. 문서는 학습용 참고자료이며, 이 레포에 실제 커밋된 구현체는 아닙니다. 직접 타이핑하며 연습하세요.

이 문서의 코드는 Spring Boot 4.1 / Spring Framework 7 기준 API를 기준으로 작성했습니다. `build.gradle`에 이미 `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa-test` 등 4.x에서 개편된 스타터 아티팩트명이 반영되어 있어 이를 따랐습니다. 다만 Spring Boot 4.x는 비교적 최근 릴리스라 세부 API가 문서 작성 시점 지식과 다를 수 있습니다. 컴파일 에러가 나면 먼저 공식 문서(https://docs.spring.io/spring-boot/reference/, https://docs.spring.io/spring-security/reference/)를 확인하세요.

---

## 목차

1. [사용자 제시 구조 분석 및 개선 제안](#1-사용자-제시-구조-분석-및-개선-제안)
2. [4~5일 학습 로드맵](#2-45일-학습-로드맵)
3. [공통 설정: 패키지 구조, application.yml](#3-공통-설정-패키지-구조-applicationyml)
4. [1단계: Member/Post/Comment CRUD + 세션 인증](#4-1단계-memberpostcomment-crud--세션-인증)
5. [2단계: 재고/주문 동시성 + N+1](#5-2단계-재고주문-동시성--n1)
6. [3단계: Spring Security 전환 + Redis 쓰기 지연](#6-3단계-spring-security-전환--redis-쓰기-지연)
7. [70분 실전 타임어택 전략](#7-70분-실전-타임어택-전략)
8. [면접 대비 Q&A](#8-면접-대비-qa)
9. [부록](#9-부록)

---

## 1. 사용자 제시 구조 분석 및 개선 제안

### 1.1 원안 요약

- 1단계: Member/Post/Comment + 세션 기반 간단 인증(bcrypt) + Post/Comment 풀 CRUD(페이징, 댓글 수 포함)
- 2단계: 재고/주문 + 동시성 이슈 재현 → 비관적 락 + N+1 재현 → fetch join + 동시성 테스트
- 3단계(선택): Spring Security 인증/인가 + Redis 쓰기 지연

### 1.2 장점

- **난이도 곡선이 합리적입니다.** 기본기(CRUD) → 실무형 함정(락, N+1) → 프레임워크 심화(Security, 캐싱) 순서는 실제 실력이 느는 순서와 일치합니다. 특히 세션 기반 수동 인증을 먼저 만들어보고 3단계에서 Spring Security로 대체하는 구성은, Security가 내부적으로 뭘 대신 해주는지 체감하게 해준다는 점에서 교육적으로 타당합니다.
- **2단계에 락과 N+1을 묶은 것도 합리적입니다.** 두 문제 모두 "정상 동작하는 것처럼 보이지만 실무에서 터지는" 유형이라, 과제형 테스트에서 실제로 자주 요구되는 패턴입니다.

### 1.3 우려되는 지점

- **1단계가 실제로는 가장 무겁습니다.** 엔티티 2개(Post/Comment) CRUD에 페이징, 댓글 수 집계 쿼리, 세션 인증 인프라(인터셉터/커스텀 어노테이션/예외처리 공통화)까지 포함하면 코드량이 2·3단계보다 많습니다. "1단계는 기본이니 하루면 되겠지"라고 시간을 짧게 잡으면 뒤로 갈수록 시간이 밀립니다. → 아래 로드맵에서는 1단계에 이틀을 배정했습니다.
- **"댓글 수 포함 목록 조회"는 그 자체로 N+1 함정입니다.** 사용자는 N+1을 2단계 주제로만 명시했지만, 1단계의 게시글 목록에서 댓글 수를 즉시로딩처럼 순회 조회하면 이미 N+1이 발생합니다. 이 문서는 1단계에서 집계 쿼리(DTO 프로젝션)로 처음부터 회피하고, "왜 이것도 N+1이 될 수 있는지"는 2단계에서 원리와 함께 재설명하는 방식으로 두 번 반복 학습되게 구성했습니다.
- **3단계의 두 주제(Security, Redis)는 서로 독립적입니다.** "시간 남으면"으로 묶어두면 어느 쪽부터 해야 할지 애매합니다. 실제 과제형 테스트도 보통 인증/인가 요구사항과 캐싱 요구사항이 같이 나오는 경우는 드물고 둘 중 하나만 나옵니다. → 두 트랙을 독립적으로 다루고, 본인이 약한 쪽을 먼저 연습하도록 안내합니다.
- **테스트 코드 요구가 2단계(동시성)에만 명시되어 있습니다.** 실제 채점 기준에는 기본 CRUD에 대한 API 테스트(MockMvc)도 흔히 포함됩니다. → 1단계에도 최소한의 MockMvc 테스트를 추가했습니다.
- **처음부터 다시 만들어보는 모의고사 단계가 빠져있습니다.** 가이드를 보며 따라 만드는 것과, 70분 안에 아무것도 안 보고 만드는 것은 완전히 다른 능력입니다. → 5일차 오후에 실전 모의고사를 배치했습니다.

### 1.4 개선 제안 (반영된 구조)

원안의 큰 틀(1→2→3단계)은 유지하되, 다음을 변경합니다.

| 구분 | 원안 | 개선안 |
|---|---|---|
| 1단계 배정 시간 | 암묵적으로 1일 | 명시적으로 1.5~2일 |
| 공통 인프라 | 특별히 언급 없음 | 1단계 초반에 예외처리·BaseTimeEntity·인증 인프라를 "재사용 가능한 템플릿"으로 명시적으로 분리 작성 → 이후 실전에서 복붙 |
| 1단계 테스트 | 없음 | MockMvc 기반 API 테스트 최소 세트 추가 |
| 3단계 | Security + Redis 묶음 | 두 개의 독립 트랙으로 분리, 취약한 쪽 우선 선택 |
| 마무리 | 없음 | 처음부터 다시 만드는 70분 모의고사 2회 |

---

## 2. 4~5일 학습 로드맵

하루 6시간 기준, 50분 학습 + 10분 휴식의 포모도로 6세트 정도로 잡으면 됩니다.

### Day 1 (6h) — 공통 인프라 + Member
- (1h) 프로젝트 구조 이해, 패키지 전략 결정 (3장)
- (1h) BaseTimeEntity, ErrorCode/BusinessException/GlobalExceptionHandler 작성 → **이건 앞으로 어떤 문제가 나와도 그대로 복붙할 개인 템플릿입니다.**
- (2h) Member 엔티티, 회원가입(bcrypt 인코딩), 세션 로그인/로그아웃, `@Login` 커스텀 어노테이션 + ArgumentResolver
- (2h) curl로 회원가입/로그인 직접 테스트, 트러블슈팅

### Day 2 (6h) — Post/Comment CRUD
- (2h) Post 엔티티/Repository, 작성·상세조회(댓글 페이징 포함)·수정·삭제
- (2h) Comment 엔티티/Repository, 작성·수정·삭제, 게시글 목록의 댓글 수 집계 쿼리
- (1h) MockMvc API 테스트 작성
- (1h) 1단계 전체 복습 + 개인 템플릿 정리 (요청 검증, 예외 코드 등)

### Day 3 (6h) — 재고/주문 + N+1
- (1h) Product/Order/OrderItem 설계
- (2h) 주문 목록 조회에서 N+1 재현 (SQL 로그로 직접 확인) → fetch join으로 해결, distinct/페이징 이슈까지 확인
- (2h) 재고 차감 동시성 버그 재현 (락 없이) → 결과가 어긋나는 것을 직접 확인
- (1h) 비관적 락(`@Lock(PESSIMISTIC_WRITE)`) 적용

### Day 4 (6h) — 동시성 테스트 + 2단계 복습
- (2h) `ExecutorService` + `CountDownLatch` 동시성 테스트 작성, 락 적용 전/후 비교
- (1h) 낙관적 락과의 차이 정리 (심화)
- (1h) N+1 대안 비교: fetch join vs `@EntityGraph` vs `@BatchSize` vs DTO 프로젝션
- (2h) 2단계 처음부터 다시 구현 (안 보고 최대한 재현)

### Day 5 (6h) — Security/Redis + 실전 모의고사
- (오전 3h) 본인이 약한 쪽부터: Spring Security 전환(세션 인증 → `UserDetailsService`) 또는 Redis 쓰기 지연 캐싱 중 택1 우선, 여유되면 나머지
- (오후 3h) **70분 타임어택 모의고사 2회.** 1회차는 1단계 스펙(게시판 CRUD)만, 2회차는 2단계 스펙(재고/주문 + 락)만. 가이드 보지 않고 진행 후, 끝나고 15분씩 셀프 리뷰.

시간이 5일이 아니라 4일이라면 Day 3~4를 하루로 압축하고(동시성 테스트는 필수, 낙관적 락 비교는 생략), Day 5의 오전 트랙 중 하나만 선택하세요.

---

## 3. 공통 설정: 패키지 구조, application.yml

### 3.1 패키지 구조: 계층형 vs 도메인형

두 가지 방식이 있습니다.

**계층형 (Layer-by-layer)** — 이 문서가 채택한 방식. `com.example.demo_260815` 하위에 `controller`, `service`, `repository`, `domain`, `dto`, `global`로 나눕니다.

**도메인형 (Package-by-feature)** — `domain.post`, `domain.comment` 처럼 기능 단위로 묶고 그 안에 Controller/Service/Repository를 함께 둡니다.

| 기준 | 계층형 | 도메인형 |
|---|---|---|
| 코딩테스트 적합성 | 파일 위치가 예측 가능해서 빠르게 이동 가능, 채점자가 보기 편함 | 처음엔 낯설어서 탐색 시간이 더 걸릴 수 있음 |
| 대규모 협업 | 도메인이 늘어날수록 한 패키지(예: service)에 파일이 난립 | 도메인 경계가 명확, MSA 분리 시 유리 |
| 결합도 | 도메인 간 경계가 흐려지기 쉬움 | 도메인 응집도가 높음 |

과제형 코딩테스트는 규모가 작고 채점자가 빠르게 훑어봐야 하므로, **계층형을 기본으로 추천**합니다. 다만 면접에서 "왜 이 구조를 썼냐"는 질문에는 위 트레이드오프로 답변하면 됩니다.

최종 패키지 구조:

```
com.example.demo_260815
├── Demo260815Application.java
├── controller
│   ├── AuthController.java
│   ├── MemberController.java
│   ├── PostController.java
│   ├── CommentController.java
│   ├── ProductController.java
│   └── OrderController.java
├── service
│   ├── MemberService.java
│   ├── PostService.java
│   ├── CommentService.java
│   ├── ProductService.java
│   └── OrderService.java
├── repository
│   ├── MemberRepository.java
│   ├── PostRepository.java
│   ├── CommentRepository.java
│   ├── ProductRepository.java
│   └── OrderRepository.java
├── domain
│   ├── BaseTimeEntity.java
│   ├── member/Member.java
│   ├── post/Post.java
│   ├── comment/Comment.java
│   ├── product/Product.java
│   └── order/Order.java, OrderItem.java, OrderStatus.java
├── dto
│   ├── member/ (MemberSignUpRequest, LoginRequest)
│   ├── post/ (PostCreateRequest, PostUpdateRequest, PostSummaryDto, PostDetailResponse)
│   ├── comment/ (CommentCreateRequest, CommentUpdateRequest, CommentResponse)
│   └── order/ (OrderCreateRequest, OrderResponse)
└── global
    ├── auth/ (Login, LoginMemberArgumentResolver, SessionConst, WebConfig)
    ├── config/ (SecurityConfig, RedisConfig)
    └── exception/ (ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler)
```

### 3.2 application.yml (1~2단계 공통)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;LOCK_TIMEOUT=10000
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100

logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.bind: trace
```

- `show-sql: true` + `org.hibernate.SQL: debug`는 N+1을 눈으로 확인하기 위한 필수 설정입니다. 실전 코딩테스트에서도 켜두고 시작하세요.
- `LOCK_TIMEOUT=10000`은 2단계 비관적 락 테스트에서 락 대기 타임아웃을 넉넉히 주기 위한 설정입니다. H2는 기본 락 타임아웃이 짧아서 동시성 테스트 중 불필요한 예외가 날 수 있습니다.
- `default_batch_fetch_size: 100`은 N+1의 또 다른 대안(배치 in절 로딩)을 위한 설정이며 5.3절에서 다시 설명합니다.

### 3.3 BaseTimeEntity (공통 감사 필드)

```java
package com.example.demo_260815.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

메인 클래스에 `@EnableJpaAuditing`을 추가해야 동작합니다.

```java
package com.example.demo_260815;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling // 3단계 Redis 쓰기 지연 스케줄러용. 1~2단계만 할 거면 없어도 무방
@SpringBootApplication
public class Demo260815Application {

    public static void main(String[] args) {
        SpringApplication.run(Demo260815Application.class, args);
    }
}
```

**왜 `@MappedSuperclass`인가**: 이 클래스 자체는 테이블이 되지 않고, 상속받는 엔티티(Post, Comment 등)의 컬럼으로 병합됩니다. `@Entity`로 두고 상속 매핑(`@Inheritance`)을 쓰는 것과 헷갈리기 쉬운데, 감사 필드처럼 "테이블을 갖지 않는 공통 필드 묶음"에는 `@MappedSuperclass`가 정석입니다.

---

## 4. 1단계: Member/Post/Comment CRUD + 세션 인증

### 4.1 설계 요약

- `Member`: 로그인 아이디, bcrypt로 인코딩된 비밀번호, 닉네임
- `Post`: 제목, 내용, 작성자(Member), 감사 필드
- `Comment`: 내용, 작성자(Member), 소속 게시글(Post), 감사 필드
- 인증은 **Spring Security의 인증 기능은 쓰지 않고** HttpSession 기반 수동 로그인으로 구현합니다. 단, `spring-boot-starter-security`가 이미 클래스패스에 있으므로 그대로 두면 모든 요청에 기본 로그인 폼이 걸립니다. 이를 막기 위한 최소 SecurityConfig(전체 permitAll)를 함께 작성합니다.
- 본인 글/댓글만 수정·삭제 가능하도록 소유자 검증을 포함합니다. (문제에 명시되어 있지 않아도 인증 시스템을 만든 이상 자연스럽게 따라오는 요구사항이라 가정하고 구현합니다. 실제 문제에서 요구하지 않으면 이 부분만 제거하면 됩니다.)

### 4.2 예외 처리 공통 인프라

```java
package com.example.demo_260815.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    NO_PERMISSION(HttpStatus.FORBIDDEN, "본인이 작성한 글/댓글만 처리할 수 있습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
```

```java
package com.example.demo_260815.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

```java
package com.example.demo_260815.global.exception;

public record ErrorResponse(String code, String message) {
}
```

```java
package com.example.demo_260815.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_INPUT", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
```

**핵심 설명**
- `@RestControllerAdvice`는 모든 `@RestController`에서 던져진 예외를 한곳에서 가로챕니다. 컨트롤러마다 try-catch를 두지 않아도 되는 이유가 이것입니다 (AOP 기반 예외 인터셉션).
- `BusinessException` + `ErrorCode` enum 조합은 "예외 종류마다 클래스를 만들 필요 없이, enum 하나로 상태코드/메시지를 관리"하는 실무 패턴입니다. 예외 클래스를 종류별로 늘리는 대안도 있지만, 과제형 테스트처럼 시간이 촉박할 때는 이 방식이 타이핑량이 훨씬 적습니다.
- `MethodArgumentNotValidException`은 `@Valid` 검증 실패 시 Spring이 던지는 예외입니다. 이를 별도로 안 잡으면 500 에러로 응답되어 채점 시 감점 요인이 됩니다.

### 4.3 세션 인증 인프라

```java
package com.example.demo_260815.global.auth;

public class SessionConst {

    public static final String LOGIN_MEMBER_ID = "loginMemberId";

    private SessionConst() {
    }
}
```

```java
package com.example.demo_260815.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Login {
}
```

```java
package com.example.demo_260815.global.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Login.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER_ID) == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
    }
}
```

```java
package com.example.demo_260815.global.auth;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginMemberArgumentResolver loginMemberArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginMemberArgumentResolver);
    }
}
```

**핵심 설명**
- `HandlerMethodArgumentResolver`는 컨트롤러 메서드의 파라미터를 Spring이 주입해주기 전에 가로채서 커스텀 로직으로 값을 만들어주는 확장 포인트입니다. `@Login Long memberId`라고만 써도 세션에서 로그인 회원 ID를 꺼내주는 이유가 이것입니다.
- 세션이 없거나 로그인 정보가 없으면 컨트롤러 진입 전에 예외를 던지므로, 각 컨트롤러/서비스에서 "로그인했는지" 검증하는 코드를 반복할 필요가 없습니다.
- **세션 기반 인증의 한계**(면접 단골 질문): 서버가 여러 대(스케일아웃)면 세션 클러스터링이나 Redis 세션 스토어가 필요합니다. 무상태(stateless)가 아니라서 서버 확장이 번거롭습니다. 3단계에서 다루는 JWT/Security 기반 토큰 인증은 이 문제를 해결하는 대안입니다.

### 4.4 1단계 SecurityConfig (전체 허용)

```java
package com.example.demo_260815.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

`spring-boot-starter-security`가 클래스패스에 있으면 아무 설정도 안 했을 때 Spring Boot가 자동으로 모든 요청에 로그인을 요구하고 콘솔에 랜덤 비밀번호를 찍습니다. 1단계에서는 인증을 직접 세션으로 구현할 것이므로 `anyRequest().permitAll()`로 이 기본 동작을 꺼둡니다. `PasswordEncoder` 빈은 필터체인과 무관하게 bcrypt 인코딩을 위해 필요합니다. CSRF는 세션 기반이라 원래는 켜두는 것이 안전하지만, API 테스트 편의를 위해 꺼둔다는 점을 인지하고 있어야 합니다(실무라면 프론트와 CSRF 토큰을 주고받는 방식을 씁니다).

### 4.5 Member 도메인

```java
package com.example.demo_260815.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Builder
    private Member(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    public static Member create(String loginId, String encodedPassword, String nickname) {
        return Member.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .nickname(nickname)
                .build();
    }
}
```

```java
package com.example.demo_260815.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo_260815.domain.member.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
```

**왜 Setter가 없는가**: `@NoArgsConstructor(access = PROTECTED)`로 JPA 프록시 생성을 위한 기본 생성자는 열어두되 외부에서 직접 호출은 막고, `@Builder` + 정적 팩토리 메서드(`create`)로만 객체를 만들게 강제합니다. Setter로 아무 필드나 바꿀 수 있게 하면 "언제 어디서 상태가 바뀌었는지" 추적이 불가능해지므로 도메인 메서드로만 상태 변경을 허용하는 것이 원칙입니다. 비밀번호는 password 자체가 바뀔 일이 없는 도메인이라 별도 변경 메서드는 두지 않았습니다.

### 4.6 회원가입/로그인 DTO, 서비스, 컨트롤러

```java
package com.example.demo_260815.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberSignUpRequest(
        @NotBlank(message = "아이디는 필수입니다.") @Size(min = 4, max = 20) String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, max = 30) String password,
        @NotBlank(message = "닉네임은 필수입니다.") @Size(max = 30) String nickname
) {
}
```

```java
package com.example.demo_260815.dto.member;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
```

```java
package com.example.demo_260815.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.dto.member.MemberSignUpRequest;
import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;
import com.example.demo_260815.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signUp(MemberSignUpRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = Member.create(request.loginId(), encodedPassword, request.nickname());
        return memberRepository.save(member).getId();
    }
}
```

```java
package com.example.demo_260815.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo_260815.dto.member.MemberSignUpRequest;
import com.example.demo_260815.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<Long> signUp(@RequestBody @Valid MemberSignUpRequest request) {
        Long memberId = memberService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberId);
    }
}
```

```java
package com.example.demo_260815.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.dto.member.LoginRequest;
import com.example.demo_260815.global.auth.SessionConst;
import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;
import com.example.demo_260815.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/api/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequest request, HttpServletRequest servletRequest) {
        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD));
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        HttpSession session = servletRequest.getSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().build();
    }
}
```

**핵심 설명**
- `@Transactional(readOnly = true)`를 서비스 클래스 레벨에 걸고, 쓰기 메서드에만 `@Transactional`을 다시 붙이는 이유: readOnly 트랜잭션은 Hibernate가 변경 감지(dirty checking)를 위한 스냅샷을 만들지 않아 성능상 이점이 있고, 실수로 조회 메서드에서 엔티티를 변경하는 것을 방지하는 효과도 있습니다.
- `passwordEncoder.matches(원문, 인코딩값)`은 원문을 다시 인코딩해서 비교하는 게 아니라, bcrypt 해시에 포함된 salt를 이용해 검증합니다. bcrypt가 매번 다른 해시값을 만들면서도 검증이 가능한 이유가 이것입니다(단방향 해시 + salt 내장).
- `request.getSession()`(인자 없음)은 세션이 없으면 새로 만들고, `getSession(false)`는 없으면 `null`을 반환합니다. 로그인 시점엔 세션을 새로 만들어야 하니 인자 없는 버전을, 인증 확인 시점엔 세션을 함부로 생성하면 안 되니 `false`를 씁니다.

### 4.7 Post 도메인

```java
package com.example.demo_260815.domain.post;

import com.example.demo_260815.domain.BaseTimeEntity;
import com.example.demo_260815.domain.member.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    private Post(String title, String content, Member member) {
        this.title = title;
        this.content = content;
        this.member = member;
    }

    public static Post create(String title, String content, Member member) {
        return Post.builder().title(title).content(content).member(member).build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public boolean isWrittenBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}
```

**왜 `@ManyToOne(fetch = FetchType.LAZY)`인가**: JPA에서 `@ManyToOne`, `@OneToOne`의 기본 페치 전략은 `EAGER`입니다. 게시글을 하나만 조회해도 연관된 회원을 항상 즉시 조회해버리면 불필요한 조인/쿼리가 늘어나므로, **연관관계는 기본적으로 LAZY로 걸고 필요한 곳에서만 fetch join으로 즉시 로딩하는 것이 정석**입니다. 이건 5장 N+1의 핵심 배경 지식이기도 합니다.

### 4.8 Comment 도메인

```java
package com.example.demo_260815.domain.comment;

import com.example.demo_260815.domain.BaseTimeEntity;
import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.domain.post.Post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    private Comment(String content, Member member, Post post) {
        this.content = content;
        this.member = member;
        this.post = post;
    }

    public static Comment create(String content, Member member, Post post) {
        return Comment.builder().content(content).member(member).post(post).build();
    }

    public void update(String content) {
        this.content = content;
    }

    public boolean isWrittenBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}
```

### 4.9 Repository (페이징 + 댓글 수 집계)

```java
package com.example.demo_260815.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo_260815.domain.post.Post;
import com.example.demo_260815.dto.post.PostSummaryDto;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = """
            select new com.example.demo_260815.dto.post.PostSummaryDto(
                p.id, p.title, p.member.nickname, p.createdAt, count(c)
            )
            from Post p
            left join p.member
            left join Comment c on c.post = p
            group by p.id, p.title, p.member.nickname, p.createdAt
            order by p.id desc
            """,
            countQuery = "select count(p) from Post p")
    Page<PostSummaryDto> findPostSummaries(Pageable pageable);

    @Query("select p from Post p join fetch p.member where p.id = :id")
    Optional<Post> findByIdWithMember(@Param("id") Long id);
}
```

```java
package com.example.demo_260815.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo_260815.domain.comment.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c from Comment c join fetch c.member where c.post.id = :postId")
    Page<Comment> findByPostIdWithMember(@Param("postId") Long postId, Pageable pageable);

    long countByPostId(Long postId);
}
```

**핵심 설명**
- `findPostSummaries`는 게시글 목록 + 작성자 닉네임 + 댓글 수를 **쿼리 1번**으로 가져옵니다. 만약 목록을 조회한 뒤 각 게시글마다 `comment.countByPostId(post.getId())`를 반복 호출하면, 게시글이 N개일 때 카운트 쿼리가 N번 추가로 나가는 전형적인 N+1이 됩니다. DTO 프로젝션 + GROUP BY로 한 번에 집계하는 것이 여기서의 해법입니다.
- `@Query`로 복잡한 집계 쿼리를 쓸 때 Spring Data가 자동으로 만드는 count 쿼리는 GROUP BY가 섞인 쿼리에서 종종 틀리거나 에러가 납니다. 그래서 `countQuery`를 명시적으로 지정했습니다. **이건 실무에서 자주 걸리는 함정이라 반드시 기억해두세요.**
- `findByPostIdWithMember`처럼 `@ManyToOne`(ToOne) 연관관계에 fetch join을 걸면서 `Pageable`을 같이 써도 안전합니다. ToOne 관계는 결과 행 수를 늘리지 않기 때문에 DB의 LIMIT/OFFSET이 정확히 동작합니다. 반면 5장에서 다룰 `@OneToMany`(ToMany) fetch join은 페이징과 함께 쓰면 안 됩니다 — 이 차이가 면접에서 자주 나옵니다.

### 4.10 DTO

```java
package com.example.demo_260815.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 100) String title,
        @NotBlank(message = "내용은 필수입니다.") String content
) {
}
```

```java
package com.example.demo_260815.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content
) {
}
```

```java
package com.example.demo_260815.dto.post;

import java.time.LocalDateTime;

public record PostSummaryDto(
        Long postId,
        String title,
        String writerNickname,
        LocalDateTime createdAt,
        Long commentCount
) {
}
```

```java
package com.example.demo_260815.dto.comment;

import java.time.LocalDateTime;

import com.example.demo_260815.domain.comment.Comment;

public record CommentResponse(
        Long commentId,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getCreatedAt()
        );
    }
}
```

```java
package com.example.demo_260815.dto.post;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

import com.example.demo_260815.domain.post.Post;
import com.example.demo_260815.dto.comment.CommentResponse;

public record PostDetailResponse(
        Long postId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt,
        Page<CommentResponse> comments
) {
    public static PostDetailResponse of(Post post, Page<CommentResponse> comments) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getNickname(),
                post.getCreatedAt(),
                comments
        );
    }
}
```

```java
package com.example.demo_260815.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.") @Size(max = 500) String content
) {
}
```

```java
package com.example.demo_260815.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank @Size(max = 500) String content
) {
}
```

**왜 record인가**: Java 16+의 record는 불변 DTO를 한 줄로 정의할 수 있게 해줍니다. 생성자·getter(관례상 `title()` 형태)·`equals`/`hashCode`가 자동 생성됩니다. 요청/응답 DTO처럼 "값을 담기만 하고 변경할 필요 없는" 객체에 적합합니다. Bean Validation 애너테이션도 record 컴포넌트에 그대로 붙일 수 있습니다.

### 4.11 PostService / CommentService

```java
package com.example.demo_260815.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_260815.domain.comment.Comment;
import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.domain.post.Post;
import com.example.demo_260815.dto.comment.CommentResponse;
import com.example.demo_260815.dto.post.PostCreateRequest;
import com.example.demo_260815.dto.post.PostDetailResponse;
import com.example.demo_260815.dto.post.PostSummaryDto;
import com.example.demo_260815.dto.post.PostUpdateRequest;
import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;
import com.example.demo_260815.repository.CommentRepository;
import com.example.demo_260815.repository.MemberRepository;
import com.example.demo_260815.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public Long createPost(Long memberId, PostCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Post post = Post.create(request.title(), request.content(), member);
        return postRepository.save(post).getId();
    }

    public Page<PostSummaryDto> getPosts(Pageable pageable) {
        return postRepository.findPostSummaries(pageable);
    }

    public PostDetailResponse getPostDetail(Long postId, Pageable commentPageable) {
        Post post = postRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Page<CommentResponse> comments = commentRepository.findByPostIdWithMember(postId, commentPageable)
                .map(CommentResponse::from);
        return PostDetailResponse.of(post, comments);
    }

    @Transactional
    public void updatePost(Long postId, Long memberId, PostUpdateRequest request) {
        Post post = getPostOrThrow(postId);
        validateOwner(post.isWrittenBy(memberId));
        post.update(request.title(), request.content());
    }

    @Transactional
    public void deletePost(Long postId, Long memberId) {
        Post post = getPostOrThrow(postId);
        validateOwner(post.isWrittenBy(memberId));
        postRepository.delete(post);
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateOwner(boolean isOwner) {
        if (!isOwner) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
    }
}
```

```java
package com.example.demo_260815.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_260815.domain.comment.Comment;
import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.domain.post.Post;
import com.example.demo_260815.dto.comment.CommentCreateRequest;
import com.example.demo_260815.dto.comment.CommentUpdateRequest;
import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;
import com.example.demo_260815.repository.CommentRepository;
import com.example.demo_260815.repository.MemberRepository;
import com.example.demo_260815.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long createComment(Long memberId, Long postId, CommentCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Comment comment = Comment.create(request.content(), member, post);
        return commentRepository.save(comment).getId();
    }

    @Transactional
    public void updateComment(Long commentId, Long memberId, CommentUpdateRequest request) {
        Comment comment = getCommentOrThrow(commentId);
        validateOwner(comment.isWrittenBy(memberId));
        comment.update(request.content());
    }

    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = getCommentOrThrow(commentId);
        validateOwner(comment.isWrittenBy(memberId));
        commentRepository.delete(comment);
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateOwner(boolean isOwner) {
        if (!isOwner) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
    }
}
```

### 4.12 PostController / CommentController

```java
package com.example.demo_260815.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo_260815.dto.post.PostCreateRequest;
import com.example.demo_260815.dto.post.PostDetailResponse;
import com.example.demo_260815.dto.post.PostSummaryDto;
import com.example.demo_260815.dto.post.PostUpdateRequest;
import com.example.demo_260815.global.auth.Login;
import com.example.demo_260815.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Long> createPost(@Login Long memberId, @RequestBody @Valid PostCreateRequest request) {
        Long postId = postService.createPost(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    @GetMapping
    public ResponseEntity<Page<PostSummaryDto>> getPosts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.ASC) Pageable commentPageable) {
        return ResponseEntity.ok(postService.getPostDetail(postId, commentPageable));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@Login Long memberId, @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request) {
        postService.updatePost(postId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@Login Long memberId, @PathVariable Long postId) {
        postService.deletePost(postId, memberId);
        return ResponseEntity.noContent().build();
    }
}
```

```java
package com.example.demo_260815.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo_260815.dto.comment.CommentCreateRequest;
import com.example.demo_260815.dto.comment.CommentUpdateRequest;
import com.example.demo_260815.global.auth.Login;
import com.example.demo_260815.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<Long> createComment(@Login Long memberId, @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request) {
        Long commentId = commentService.createComment(memberId, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentId);
    }

    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> updateComment(@Login Long memberId, @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request) {
        commentService.updateComment(commentId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@Login Long memberId, @PathVariable Long commentId) {
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.noContent().build();
    }
}
```

**핵심 설명**
- `@PageableDefault`로 클라이언트가 `page`/`size`/`sort` 쿼리 파라미터를 안 보내도 기본값이 적용됩니다. Spring Data Web(`Pageable`)이 자동으로 `?page=0&size=10&sort=id,desc` 형태를 바인딩해줍니다.
- 댓글 목록은 상세 조회 시 함께 반환되므로, 컨트롤러 파라미터를 두 개의 `Pageable`(게시글 페이징은 안 씀, 댓글 페이징만 사용)로 받습니다. `Pageable`을 두 개 이상 받을 땐 파라미터 접두사를 구분해야 하는 경우도 있는데, 여기서는 상세조회 엔드포인트에 하나만 쓰므로 문제 없습니다. 만약 한 엔드포인트에서 `Pageable` 두 개를 동시에 받아야 한다면 `@Qualifier`로 접두사를 지정해야 한다는 점은 알아두세요.

### 4.13 curl 테스트

```bash
curl -i -X POST http://localhost:8080/api/members -H "Content-Type: application/json" -d '{"loginId":"tester01","password":"password123","nickname":"테스터"}'
```

```bash
curl -i -X POST http://localhost:8080/api/login -c cookie.txt -H "Content-Type: application/json" -d '{"loginId":"tester01","password":"password123"}'
```

```bash
curl -i -X POST http://localhost:8080/api/posts -b cookie.txt -H "Content-Type: application/json" -d '{"title":"첫 게시글","content":"내용입니다"}'
```

```bash
curl -i "http://localhost:8080/api/posts?page=0&size=10"
```

```bash
curl -i "http://localhost:8080/api/posts/1?page=0&size=5"
```

```bash
curl -i -X PUT http://localhost:8080/api/posts/1 -b cookie.txt -H "Content-Type: application/json" -d '{"title":"수정된 제목","content":"수정된 내용"}'
```

```bash
curl -i -X DELETE http://localhost:8080/api/posts/1 -b cookie.txt
```

`-c cookie.txt`로 로그인 시 세션 쿠키를 저장하고, 이후 요청에 `-b cookie.txt`로 재사용합니다. 세션 인증을 curl로 테스트할 때 자주 까먹는 부분이니 기억해두세요.

### 4.14 MockMvc 기본 API 테스트

```java
package com.example.demo_260815.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 게시글을_작성하고_목록에서_조회할_수_있다() throws Exception {
        // given: 회원가입 + 로그인
        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"tester01\",\"password\":\"password123\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated());

        var loginResult = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"tester01\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var session = loginResult.getRequest().getSession(false);

        // when: 게시글 작성
        mockMvc.perform(post("/api/posts")
                .session((jakarta.servlet.http.MockHttpSession) session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isCreated());

        // then: 목록 조회 시 댓글 수 0으로 확인
        mockMvc.perform(get("/api/posts?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", is("제목")))
                .andExpect(jsonPath("$.content[0].commentCount", is(0)));
    }
}
```

`@Transactional`을 테스트 클래스에 붙이면 각 테스트가 끝날 때 자동 롤백되어 테스트 간 데이터가 섞이지 않습니다. 과제형 테스트에서 시간이 없다면 이 정도의 통합 테스트 1~2개만 작성해도 "테스트를 작성할 줄 안다"는 것을 보여주기에 충분합니다.

### 4.15 자주 하는 실수 (1단계)

- Security 스타터가 있는데 SecurityConfig를 빼먹어서 모든 API가 401로 막힘
- `@Transactional` 없이 컨트롤러/서비스 밖에서 지연 로딩 필드(`member.getNickname()`)에 접근해서 `LazyInitializationException` 발생
- 엔티티를 그대로 `@ResponseBody`로 반환해서 양방향 연관관계 직렬화 시 무한 루프(StackOverflow) — 반드시 DTO로 변환해서 반환
- `PageableDefault`의 `sort` 속성에 존재하지 않는 필드명을 적어 부팅은 되지만 조회 시 예외 발생
- 비밀번호를 평문으로 저장(bcrypt 인코딩 누락)

---

## 5. 2단계: 재고/주문 동시성 + N+1

### 5.1 설계 요약

- `Product`: 이름, 가격, 재고 수량
- `Order`: 주문 회원, 주문 상태, 주문 상품 목록(`OrderItem`)
- `OrderItem`: 주문 상품, 수량, 주문 시점 가격(스냅샷)

먼저 "버그가 있는 버전"을 만들어 문제를 눈으로 확인한 뒤, 같은 코드를 "고친 버전"으로 바꾸는 순서로 학습합니다.

### 5.2 도메인

```java
package com.example.demo_260815.domain.product;

import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Builder
    private Product(String name, int price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public static Product create(String name, int price, int stock) {
        return Product.builder().name(name).price(price).stock(stock).build();
    }

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
    }
}
```

```java
package com.example.demo_260815.domain.order;

public enum OrderStatus {
    ORDERED, CANCELLED
}
```

```java
package com.example.demo_260815.domain.order;

import java.util.ArrayList;
import java.util.List;

import com.example.demo_260815.domain.BaseTimeEntity;
import com.example.demo_260815.domain.member.Member;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(Member member) {
        this.member = member;
        this.status = OrderStatus.ORDERED;
    }

    public static Order create(Member member) {
        return Order.builder().member(member).build();
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }
}
```

```java
package com.example.demo_260815.domain.order;

import com.example.demo_260815.domain.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int orderPrice;

    @Builder
    private OrderItem(Product product, int quantity, int orderPrice) {
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }

    public static OrderItem create(Product product, int quantity) {
        return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .orderPrice(product.getPrice())
                .build();
    }

    void assignOrder(Order order) {
        this.order = order;
    }
}
```

**왜 `orderPrice`를 따로 저장하는가**: 주문 시점의 가격을 스냅샷으로 남겨야 이후 상품 가격이 바뀌어도 과거 주문 내역의 금액이 변하지 않습니다. `product.getPrice()`를 매번 참조하면 나중에 가격이 오르내릴 때 과거 주문 금액까지 바뀌어버리는 버그가 생깁니다. 이런 "왜 필드를 중복 저장하는가"류 질문은 면접에서 자주 나옵니다.

### 5.3 N+1 재현과 해결

```java
package com.example.demo_260815.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo_260815.domain.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // [버그 재현용] 지연 로딩 상태 그대로 반환 -> orderItems, product 접근 시 N+1 발생
    List<Order> findAll();

    // [해결] fetch join으로 연관 엔티티를 한 번에 조회
    @Query("""
            select distinct o from Order o
            join fetch o.member
            join fetch o.orderItems oi
            join fetch oi.product
            """)
    List<Order> findAllWithItemsAndProduct();
}
```

**재현 방법**: `findAll()`로 주문 목록을 가져온 뒤 각 주문의 `order.getOrderItems()`를 순회하며 `orderItem.getProduct().getName()`을 출력해보세요. `show-sql: true`를 켜둔 상태에서 로그를 보면 주문 1건당 `orderItems` 조회 쿼리 1번 + 상품 조회 쿼리가 아이템 개수만큼 추가로 나가는 것을 직접 확인할 수 있습니다. 주문이 N건이면 총 쿼리 수가 `1(주문 목록) + N(각 주문의 orderItems) + M(각 orderItem의 product)`로 폭증합니다. 이게 N+1 문제입니다.

**해결**: `findAllWithItemsAndProduct()`처럼 `join fetch`로 연관 엔티티를 한 쿼리에 담아옵니다.

- `distinct`가 필요한 이유: `@OneToMany` fetch join은 SQL 레벨에서 조인이 일어나 주문 1건이 orderItem 개수만큼 행으로 뻥튀기됩니다(카티션 곱). JPQL의 `distinct`는 애플리케이션(Hibernate) 레벨에서 중복된 Order 엔티티를 제거해줍니다.
- **이 쿼리는 페이징(`Pageable`)과 함께 쓰면 안 됩니다.** `@OneToMany` 컬렉션을 fetch join하면서 `Pageable`을 넘기면 Hibernate가 "메모리에서 페이징하겠다"는 경고(`HHH000104`)를 내고 전체 데이터를 애플리케이션으로 끌고 온 뒤 자바에서 자릅니다. 데이터가 많으면 그대로 장애로 이어질 수 있습니다.

**페이징이 필요할 때의 대안** (면접에서 물어보면 이 3가지를 비교해서 답하면 됩니다):
1. **2단계 조회**: 먼저 `Order` id만 페이징으로 조회 → 그 id 목록으로 `in절` fetch join 조회. 페이징과 fetch join을 동시에 안전하게 쓰는 가장 흔한 패턴.
2. **`@BatchSize`(또는 `default_batch_fetch_size`)**: 지연 로딩을 유지하되, 연관 엔티티를 개별 쿼리 대신 `in (?, ?, ?...)`으로 묶어서 가져옵니다. 코드 변경이 거의 없다는 장점이 있습니다. `application.yml`에 이미 `default_batch_fetch_size: 100`을 설정해뒀으니, `findAll()`만 써도 N+1이 "쿼리 N번"에서 "쿼리 2~3번"으로 줄어드는 것을 확인해볼 수 있습니다.
3. **DTO 프로젝션**: 애초에 필요한 필드만 JPQL로 뽑아서 DTO로 바로 매핑. 엔티티 그래프를 아예 안 만들기 때문에 가장 가볍지만, 화면/응답 요구사항이 바뀔 때마다 쿼리를 다시 짜야 하는 유연성 저하가 있습니다.

### 5.4 동시성 이슈 재현과 비관적 락

```java
package com.example.demo_260815.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.example.demo_260815.domain.product.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 기본 조회 (락 없음) - JpaRepository가 기본 제공하는 findById 사용

    // 비관적 락으로 재고 행을 잠그고 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
```

```java
package com.example.demo_260815.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.domain.order.Order;
import com.example.demo_260815.domain.order.OrderItem;
import com.example.demo_260815.domain.product.Product;
import com.example.demo_260815.global.exception.BusinessException;
import com.example.demo_260815.global.exception.ErrorCode;
import com.example.demo_260815.repository.MemberRepository;
import com.example.demo_260815.repository.OrderRepository;
import com.example.demo_260815.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    // [버그 재현용] 락 없이 재고 차감 -> 동시 요청 시 lost update 발생 가능
    @Transactional
    public Long orderWithoutLock(Long memberId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.decreaseStock(quantity);
        return createOrder(memberId, product, quantity);
    }

    // [해결] 비관적 락으로 재고 행을 잠근 뒤 차감
    @Transactional
    public Long order(Long memberId, Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.decreaseStock(quantity);
        return createOrder(memberId, product, quantity);
    }

    private Long createOrder(Long memberId, Product product, int quantity) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Order order = Order.create(member);
        order.addOrderItem(OrderItem.create(product, quantity));
        return orderRepository.save(order).getId();
    }
}
```

**왜 lost update가 발생하는가**: `decreaseStock`은 내부적으로 "현재 재고를 읽고(READ) → 계산하고 → 다시 쓴다(WRITE)"는 두 단계로 이루어집니다. 스레드 A와 B가 거의 동시에 재고 10을 읽고, 각자 1개씩 차감해서 9로 계산한 뒤 저장하면, 실제로는 2개가 팔렸는데 최종 재고는 9가 됩니다(원래는 8이어야 함). 이렇게 한쪽의 갱신 결과가 다른 쪽에 덮어써져 사라지는 현상을 **lost update**라 부릅니다.

**비관적 락(Pessimistic Lock)의 동작**: `PESSIMISTIC_WRITE`는 SQL의 `SELECT ... FOR UPDATE`로 변환됩니다. 해당 행을 조회하는 순간 DB 레벨에서 락을 걸어, 트랜잭션이 끝날 때까지 다른 트랜잭션이 같은 행을 조회(잠금 목적)하거나 수정하지 못하게 막습니다(대기하게 됩니다). "충돌이 자주 일어날 것"이라 가정하고 미리 잠그는 방식이라 비관적이라 불립니다.

**낙관적 락(Optimistic Lock)과의 비교** (심화, 시간 남으면):
- `@Version` 필드를 엔티티에 추가하고, UPDATE 시 `WHERE version = 기존값`을 함께 검사합니다. 갱신 시점에 버전이 다르면(다른 트랜잭션이 먼저 갱신했으면) `OptimisticLockException`이 발생합니다.
- 락을 미리 걸지 않으므로 충돌이 드문 상황에서는 성능이 좋지만, 충돌 시 재시도 로직을 애플리케이션이 직접 구현해야 합니다.
- **선택 기준**: 재고 차감처럼 충돌(동시 접근) 빈도가 높고 정합성이 중요한 경우 비관적 락, 게시글 조회수처럼 충돌이 드물고 약간의 유실이 허용되는 경우 낙관적 락 또는 락 없는 방식이 일반적입니다.

### 5.5 동시성 테스트 코드

```java
package com.example.demo_260815.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.domain.product.Product;
import com.example.demo_260815.repository.MemberRepository;
import com.example.demo_260815.repository.ProductRepository;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    OrderService orderService;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void 락_없이_동시_주문시_재고가_정확히_차감되지_않을_수_있다() throws InterruptedException {
        // given
        Product product = productRepository.save(Product.create("한정판 티셔츠", 10000, 10));
        Member member = memberRepository.save(Member.create("tester", "encoded", "테스터"));
        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 재고 10개짜리 상품에 30번 동시 주문 시도 (락 없음)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.orderWithoutLock(member.getId(), product.getId(), 1);
                } catch (Exception ignored) {
                    // 재고 부족 등 정상적인 예외는 무시
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then: 락이 없으면 결과가 0이 아니거나(레이스 컨디션으로 초과 차감/차감 누락) 예측과 달라질 수 있음
        Product result = productRepository.findById(product.getId()).orElseThrow();
        System.out.println("[락 없음] 최종 재고 = " + result.getStock() + " (기대값 0)");
    }

    @Test
    void 비관적_락을_적용하면_동시_주문에도_재고가_정확히_차감된다() throws InterruptedException {
        // given
        Product product = productRepository.save(Product.create("한정판 티셔츠", 10000, 10));
        Member member = memberRepository.save(Member.create("tester2", "encoded", "테스터2"));
        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 재고 10개짜리 상품에 30번 동시 주문 시도 (비관적 락 적용)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.order(member.getId(), product.getId(), 1);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then: 30번 시도 중 재고만큼(10번)만 성공하여 정확히 0이 되어야 함
        Product result = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.getStock()).isEqualTo(0);
    }
}
```

**핵심 설명**
- `CountDownLatch`는 "모든 스레드가 작업을 끝낼 때까지 메인 스레드를 대기시키는" 동기화 도구입니다. `threadCount`만큼 카운트를 설정해두고, 각 스레드가 끝날 때 `countDown()`을 호출하면 카운트가 0이 될 때까지 `latch.await()`에서 대기합니다. 이게 없으면 메인 스레드가 검증(`assertThat`)을 먼저 실행해버려 테스트가 무의미해집니다.
- `ExecutorService`의 스레드 풀 크기(여기선 10)와 요청 수(30)를 다르게 준 이유: 스레드 풀이 요청 수만큼 크지 않아도(즉 진짜 30개가 동시에 실행되지 않아도) 여러 스레드가 짧은 시간에 몰려 락 경합을 재현하기엔 충분합니다. 실전에서는 풀 크기를 요청 수와 비슷하게 주면 경합이 더 잘 재현됩니다.
- 락이 없는 테스트는 타이밍에 따라 우연히 정답(0)이 나올 수도 있습니다. 이건 정상입니다 — 레이스 컨디션은 "항상" 재현되는 게 아니라 "재현될 수 있는" 문제라는 것 자체가 핵심 개념입니다. 여러 번 반복 실행해서 값이 흔들리는 것을 직접 확인해보세요.
- `@SpringBootTest`는 트랜잭션을 테스트별로 롤백하지 않는 통합 테스트입니다 (`@Transactional`을 일부러 안 붙였습니다). 동시성 테스트는 여러 스레드가 각자 별도 트랜잭션을 가져야 하므로, 테스트 메서드 자체를 하나의 트랜잭션으로 감싸면 안 됩니다. `@Transactional`을 실수로 클래스에 붙이면 락 경합이 아예 재현되지 않을 수 있습니다.

### 5.6 curl 테스트

```bash
curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d '{"name":"한정판 티셔츠","price":10000,"stock":10}'
```

```bash
curl -i -X POST http://localhost:8080/api/orders -b cookie.txt -H "Content-Type: application/json" -d '{"productId":1,"quantity":1}'
```

```bash
curl -i "http://localhost:8080/api/orders"
```

(ProductController/OrderController는 4장의 컨트롤러 패턴과 동일하게 작성하면 됩니다. 지면 관계상 반복 생략하며, 필요하면 PostController 구조를 그대로 따라 만드세요.)

### 5.7 자주 하는 실수 (2단계)

- 재고 검증(`if stock < quantity`)과 차감을 서비스 레이어에서 따로 하다가, 검증 이후 차감 사이에 다른 스레드가 끼어들 틈을 만듦 → 반드시 도메인 메서드(`decreaseStock`) 안에서 검증+차감을 원자적으로 처리
- `@Lock`을 Repository 메서드에 걸었는데, 실제로는 그 메서드를 안 쓰고 기본 `findById`를 호출해서 락이 걸리지 않음
- fetch join에서 `distinct`를 빼먹어 중복된 Order가 그대로 반환됨
- `@OneToMany` fetch join 쿼리에 `Pageable`을 그대로 넘겨서 경고 로그와 함께 메모리 페이징이 발생
- 동시성 테스트에 `@Transactional`을 붙여서 락 경합이 재현되지 않음

---

## 6. 3단계: Spring Security 전환 + Redis 쓰기 지연

시간이 부족하면 본인이 약한 트랙 하나만 선택하세요. 두 트랙은 서로 독립적입니다.

### 6.1 트랙 A: Spring Security로 전환

1단계의 세션 기반 수동 인증을 Spring Security의 폼 로그인 방식으로 대체합니다.

```java
package com.example.demo_260815.global.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo_260815.domain.member.Member;

public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    public Long getMemberId() {
        return member.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getLoginId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

```java
package com.example.demo_260815.global.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo_260815.domain.member.Member;
import com.example.demo_260815.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다."));
        return new CustomUserDetails(member);
    }
}
```

```java
package com.example.demo_260815.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/api/logout"));
        return http.build();
    }
}
```

**변경 포인트**
- `UserDetailsService`는 Spring Security가 로그인 시 "이 아이디를 가진 사용자가 실제로 있는지, 비밀번호는 무엇인지"를 조회하기 위해 호출하는 인터페이스입니다. 직접 세션에 넣던 로직을 Security 필터체인이 대신 처리합니다.
- 컨트롤러에서 로그인 회원을 꺼낼 때는 `@Login Long memberId` 대신 `@AuthenticationPrincipal CustomUserDetails userDetails`를 받고 `userDetails.getMemberId()`를 사용합니다. 4장의 `LoginMemberArgumentResolver`/`@Login`은 더 이상 필요 없습니다.
- `@EnableMethodSecurity` + `@PreAuthorize("isAuthenticated()")`처럼 메서드 단위로도 인가 규칙을 걸 수 있습니다. URL 기반(`authorizeHttpRequests`)과 메서드 기반(`@PreAuthorize`) 인가의 차이를 설명할 수 있으면 면접에서 좋은 인상을 줍니다.
- 세션 기반 인증(4장) vs Spring Security 폼 로그인(여기)의 차이는 사실 "누가 인증 상태를 관리하느냐"입니다. Security도 내부적으로는 `SecurityContext`를 세션에 저장하는 방식(기본값)을 씁니다. 진짜 무상태(stateless) 인증을 하려면 세션 대신 JWT를 발급하고 매 요청마다 필터에서 토큰을 검증하는 방식으로 바꿔야 하는데, 이건 이 문서 범위를 벗어나는 추가 학습 주제입니다.

### 6.2 트랙 B: Redis 쓰기 지연 (게시글 조회수)

**패턴**: 조회수처럼 갱신이 매우 잦고 즉시 정합성이 중요하지 않은 값은, 매 요청마다 DB를 갱신하지 않고 Redis에 먼저 반영한 뒤 일정 주기로 DB에 몰아서 반영합니다. 이것을 **write-behind(쓰기 지연)** 캐싱이라 부릅니다.

`build.gradle`에 의존성을 추가합니다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

`application.yml`에 추가합니다.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Post 엔티티에 조회수 필드를 추가합니다 (4장 코드에 추가).

```java
@Column(nullable = false)
private long viewCount;

public void addViewCount(long count) {
    this.viewCount += count;
}
```

```java
package com.example.demo_260815.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

```java
package com.example.demo_260815.service;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostViewCountService {

    private static final String VIEW_COUNT_KEY_PREFIX = "post:viewcount:";

    private final RedisTemplate<String, String> redisTemplate;

    public void increaseViewCount(Long postId) {
        redisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + postId);
    }

    public long getViewCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    public Set<String> getAllViewCountKeys() {
        return redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
    }

    public Long extractPostId(String key) {
        return Long.parseLong(key.substring(key.lastIndexOf(":") + 1));
    }

    public void clear(String key) {
        redisTemplate.delete(key);
    }
}
```

```java
package com.example.demo_260815.global.scheduler;

import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_260815.repository.PostRepository;
import com.example.demo_260815.service.PostViewCountService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ViewCountFlushScheduler {

    private final PostViewCountService viewCountService;
    private final PostRepository postRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void flush() {
        Set<String> keys = viewCountService.getAllViewCountKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Long postId = viewCountService.extractPostId(key);
            long count = viewCountService.getViewCount(key);
            postRepository.findById(postId).ifPresent(post -> post.addViewCount(count));
            viewCountService.clear(key);
        }
    }
}
```

게시글 상세 조회 시 `postViewCountService.increaseViewCount(postId)`를 `PostService.getPostDetail`에서 호출하도록 한 줄만 추가하면 됩니다.

**핵심 설명**
- **왜 매번 DB에 UPDATE를 안 하는가**: 조회수는 인기 게시글의 경우 초당 수십~수백 번 갱신될 수 있는데, 매번 DB row를 UPDATE하면 그 행에 락 경합이 몰리고 DB 부하가 커집니다. Redis의 `INCR`(원자적 증가 연산)은 매우 가볍기 때문에 우선 Redis에서 흡수하고, DB에는 몰아서 한 번에 반영합니다.
- **캐싱 전략 비교** (면접 단골): Look-aside(캐시 미스 시 DB 조회 후 캐시에 채움, 가장 흔한 캐시 조회 패턴), Write-through(쓰기 시 캐시와 DB를 동시에 갱신, 정합성은 높지만 쓰기 지연), Write-behind/write-back(캐시에 먼저 쓰고 나중에 DB에 비동기 반영, 여기서 구현한 방식 — 쓰기 성능은 좋지만 캐시가 죽으면 반영 전 데이터를 잃을 수 있음).
- **트레이드오프를 명확히 인지해야 합니다**: 스케줄러가 돌기 전에 서버가 재시작되면 Redis에만 쌓인 조회수는 유실됩니다. 조회수처럼 "약간 유실되어도 되는" 데이터에만 이 패턴을 쓰는 것이 맞고, 주문 금액 같은 데이터에 write-behind를 쓰면 안 됩니다.
- `@Scheduled(fixedDelay = 60000)`는 이전 실행이 끝난 시점부터 60초 후 다시 실행합니다(`fixedRate`는 시작 시점 기준이라 실행 시간이 길어지면 겹쳐 실행될 수 있어 다름을 구분해 알아두세요). 스케줄러가 동작하려면 메인 클래스에 `@EnableScheduling`이 있어야 합니다(3.3절에 이미 추가해뒀습니다).

---

## 7. 70분 실전 타임어택 전략

### 7.1 시간 배분 가이드

| 구간 | 시간 | 할 일 |
|---|---|---|
| 요구사항 파악 | 0~5분 | 요구사항을 읽으며 엔티티와 API 스펙을 종이/메모에 스케치. 애매한 요구사항은 가정을 명시 |
| 뼈대 세팅 | 5~15분 | 엔티티, Repository 작성 → 컴파일 확인 (여기서 이 문서의 BaseTimeEntity/예외처리 템플릿을 그대로 복붙) |
| 기본 CRUD | 15~35분 | Service, Controller의 Create/Read/Update/Delete 구현 |
| 부가 요구사항 | 35~45분 | 페이징, 정렬, 검색, 집계 등 |
| 예외/검증 | 45~55분 | `@Valid`, 커스텀 예외, 권한 체크, 엣지 케이스(없는 id, 빈 문자열 등) |
| 직접 호출 검증 | 55~65분 | curl/Postman으로 실제 호출, 발견되는 버그 즉시 수정 |
| 마무리 | 65~70분 | 요구사항 재확인, 커밋 |

### 7.2 자주 나오는 실수 체크리스트

- [ ] 엔티티에 Setter를 남발해 캡슐화가 깨지지 않았는가
- [ ] 지연 로딩 필드를 트랜잭션 밖에서 접근해 `LazyInitializationException`이 나지 않는가
- [ ] 엔티티를 그대로 응답으로 반환해 무한 순환 참조/원치 않는 필드 노출이 없는가 (DTO 변환 필수)
- [ ] 페이징 파라미터(`page`, `size`, `sort`)가 없을 때 기본값이 적절히 동작하는가
- [ ] 404(존재하지 않는 리소스), 403(권한 없음), 400(검증 실패)이 각각 구분되어 응답되는가
- [ ] N+1이 발생할 만한 목록 조회 지점을 인지하고 있는가
- [ ] 컴파일 에러를 방치한 채 계속 코드를 쌓지 않았는가 (자주 빌드/재기동해서 확인)
- [ ] 의미 있는 단위로 커밋했는가 (마지막에 몰아서 하다 시간 부족으로 커밋 못 하는 경우 방지)

### 7.3 개인 템플릿 준비 권장 목록

다음 파일들은 요구사항과 무관하게 거의 그대로 재사용 가능하므로, 연습을 마친 뒤 별도 스니펫으로 저장해두고 실전에서 그대로 붙여넣으세요.

- `BaseTimeEntity`, `@EnableJpaAuditing` 설정
- `ErrorCode`, `BusinessException`, `ErrorResponse`, `GlobalExceptionHandler`
- 세션 인증이 필요할 때: `SessionConst`, `@Login`, `LoginMemberArgumentResolver`, `WebConfig`
- `application.yml`의 H2 + JPA 로깅 설정
- 동시성 테스트의 `ExecutorService` + `CountDownLatch` 보일러플레이트

---

## 8. 면접 대비 Q&A

**Q. Controller-Service-Repository 계층을 나누는 이유는?**
관심사 분리(SoC)입니다. Controller는 HTTP 요청/응답 변환만, Service는 트랜잭션 경계와 비즈니스 로직, Repository는 영속성 처리만 담당합니다. 테스트 시 각 계층을 독립적으로 모킹해서 검증할 수 있고, 예를 들어 웹 프레임워크를 바꿔도 Service/Repository는 그대로 재사용할 수 있습니다.

**Q. 영속성 컨텍스트란 무엇이고, 변경 감지(dirty checking)는 어떻게 동작하나요?**
영속성 컨텍스트는 엔티티를 관리하는 1차 캐시입니다. 트랜잭션 안에서 조회한 엔티티는 영속성 컨텍스트에 스냅샷과 함께 보관되고, 트랜잭션 커밋 시점(정확히는 flush 시점)에 현재 엔티티 상태와 스냅샷을 비교해 변경된 필드가 있으면 자동으로 UPDATE 쿼리를 만듭니다. 그래서 `post.update(title, content)`처럼 도메인 메서드로 필드만 바꿔도 별도의 `save()` 호출 없이 DB에 반영됩니다.

**Q. `@Transactional(readOnly = true)`를 서비스 클래스에 기본으로 걸어둔 이유는?**
readOnly 트랜잭션에서는 Hibernate가 변경 감지를 위한 스냅샷 비교를 생략(플러시 모드 최적화)해 성능 이점이 있고, 실수로 조회 로직에서 엔티티를 변경하는 것을 방지하는 안전장치 역할도 합니다. 쓰기가 필요한 메서드에만 `@Transactional`을 다시 붙여 재정의합니다.

**Q. N+1 문제가 왜 발생하고, 어떻게 해결하나요?**
연관관계를 지연 로딩(LAZY)으로 설정한 상태에서, 부모 엔티티 목록(N개)을 조회한 뒤 각 부모의 연관 엔티티에 접근하면 그 개수만큼 추가 쿼리가 발생합니다(총 1+N번). Fetch join으로 한 쿼리에 묶어 가져오거나, `@EntityGraph`, `@BatchSize`(in절 배치 로딩), 필요한 필드만 뽑는 DTO 프로젝션으로 해결합니다. 다만 `@OneToMany` fetch join은 페이징과 함께 쓰면 메모리 페이징 경고가 발생하므로, 페이징이 필요하면 ID만 먼저 페이징 조회 후 in절로 fetch join하는 2단계 조회를 씁니다.

**Q. 비관적 락과 낙관적 락의 차이는? 언제 무엇을 쓰나요?**
비관적 락은 DB의 `SELECT ... FOR UPDATE`로 데이터를 읽는 시점에 행을 잠그고, 낙관적 락은 `@Version` 필드로 갱신 시점에 충돌 여부만 검사합니다. 충돌이 잦고 정합성이 중요한 재고 차감 같은 경우 비관적 락이, 충돌이 드문 경우 낙관적 락(+ 재시도 로직)이 적합합니다. 비관적 락은 락 대기로 인한 처리량 저하가 있고, 낙관적 락은 충돌 시 예외를 잡아 재시도하는 로직을 애플리케이션이 직접 구현해야 합니다.

**Q. 세션 기반 인증과 토큰(JWT) 기반 인증의 차이는?**
세션은 서버가 로그인 상태를 메모리(또는 세션 스토어)에 들고 있는 상태(stateful) 방식이라, 서버를 여러 대로 확장하려면 세션 클러스터링이나 Redis 등 별도 세션 스토어가 필요합니다. JWT는 인증 정보를 토큰 자체에 서명해서 담기 때문에 서버가 상태를 들고 있지 않아도 됩니다(stateless). 대신 토큰 탈취 시 만료 전까지 무효화가 어렵다는 단점이 있어 짧은 만료시간 + 리프레시 토큰 조합을 흔히 씁니다.

**Q. bcrypt로 비밀번호를 해싱하는 이유는? 단순 SHA-256과 뭐가 다른가요?**
bcrypt는 해시마다 다른 salt를 자동으로 포함하고, 반복 연산 비용(cost factor)을 조절할 수 있어 브루트포스/레인보우 테이블 공격에 강합니다. SHA-256 같은 범용 해시 함수는 계산이 너무 빨라서 대량의 후보 비밀번호를 빠르게 대입 공격할 수 있다는 약점이 있습니다.

**Q. Redis를 캐시로 쓸 때 Look-aside, Write-through, Write-behind의 차이는?**
Look-aside는 조회 시 캐시를 먼저 보고 없으면 DB에서 읽어 캐시에 채우는 방식(가장 일반적인 조회 캐싱), Write-through는 쓰기 시 캐시와 DB를 동시에 갱신해 정합성이 높은 대신 쓰기 지연이 생기는 방식, Write-behind는 캐시에 먼저 쓰고 나중에 배치로 DB에 반영해 쓰기 성능은 좋지만 캐시 장애 시 미반영분을 잃을 수 있는 방식입니다. 조회수처럼 정합성 요구가 낮은 데이터에 Write-behind가 적합합니다.

---

## 9. 부록

### 9.1 응답 포맷에 대한 참고

이 문서는 모든 응답을 `ResponseEntity<T>`로 직접 반환하는 방식을 썼습니다. 일부 과제형 테스트는 `{ "success": true, "data": ..., "error": null }` 형태의 공통 래퍼(`ApiResponse<T>`)를 요구하기도 합니다. 문제에서 응답 포맷을 명시하지 않았다면 굳이 래퍼를 추가로 만들 필요는 없습니다(불필요한 추상화). 요구사항에 명시되어 있다면 `ApiResponse<T>` record 하나만 추가하고 컨트롤러 반환 타입을 감싸주면 됩니다.

또한 Spring Data의 `Page<T>`를 그대로 직렬화하면 `pageable`, `sort` 등 내부 구현 세부사항까지 노출됩니다. 문제에서 `{ content, page, size, totalElements, totalPages }` 형태의 단순화된 페이징 응답을 요구하면, 아래처럼 별도 DTO로 한 번 감싸면 됩니다.

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }
}
```

### 9.2 공식 문서 레퍼런스

- Spring Boot: https://docs.spring.io/spring-boot/reference/
- Spring Data JPA (쿼리 메서드, `@Query`, `@Lock`): https://docs.spring.io/spring-data/jpa/reference/
- Spring Security: https://docs.spring.io/spring-security/reference/
- Redis: https://redis.io/docs/latest/

### 9.3 이 문서의 한계

- 실제로 컴파일/실행해보지 않은 코드입니다. Spring Boot 4.1 기준 API 세부사항이 다를 수 있으니, 막히는 부분은 위 공식 문서로 반드시 교차 확인하세요.
- H2 기준으로만 작성되어 있어, 실제 MySQL/PostgreSQL과는 락 타임아웃, 격리 수준 기본값 등에서 미세한 동작 차이가 있을 수 있습니다.
- 이 문서에 없는 요구사항(검색, 좋아요, 알림 등)이 실전에서 나오면, 이 문서의 패턴(도메인 메서드 + 계층 분리 + 예외 코드화)을 그대로 응용하면 됩니다.
