# Expo Pass

> 본 프로젝트는 팀 프로젝트로 원본 저장소는 비공개(Private)이며,
> 이 저장소는 포트폴리오 공개용으로 민감정보를 제거하고 정리한 버전입니다.

여러 박람회/행사를 한 플랫폼에서 등록·홍보·예약·결제·입장까지 관리하는 행사 예약 관리 SaaS입니다. 사내 중앙 인증(auth-core)·실시간 메신저(STOMP) 인프라 위에, 행사 등록부터 QR 티켓 발급, 현장 체크인, 정산까지 이어지는 플로우를 구현했습니다.

## 주요 기능

- **행사 관리**: 행사/티켓/콘텐츠/이미지 CRUD, 카테고리, 담당 매니저 배정 (Event Admin ↔ Super Admin)
- **예약**: 티켓 재고 차감과 함께 티켓 단위 QR 발급, Redis 기반 대기열(SSE), 무료 티켓은 결제 없이 즉시 확정
- **결제/정산**: 토스페이먼츠 위젯 연동 실결제, 서버 측 금액 재검증, 행사 종료일 기준 자동 정산(수수료 5%), 예약일까지 남은 기간에 따른 단계별 자동 환불 정책
- **현장 체크인**: 카메라 QR 스캔(`jsQR`) + 수동 입력을 지원하는 매니저용 체크인 화면, 실시간 체크인 현황판
- **배너 광고**: 슬롯×기간 정가제 광고 등록, 노출/클릭 추적과 CTR 통계(XLSX 내보내기), 광고 종료 시 자동 정산
- **회원/권한 관리**: 사내 중앙 인증 연동, 역할 기반(`ADMIN`/`MANAGER`/`USER`/`DEVELOPER`) 접근 제어, 관리자용 회원-역할 관리 화면
- **실시간 메신저**: STOMP 기반 1:1/그룹 채팅, 접속 상태 표시

도메인 간 연동(결제 확정→예약 확정, 광고 반려→환불, 행사 종료→정산 등)은 Kafka가 아니라 Spring Modulith의 도메인 이벤트(`@ApplicationModuleListener`)로 처리합니다. 세부 아키텍처는 [`CLAUDE.md`](CLAUDE.md)와 하위 [`backend/CLAUDE.md`](backend/CLAUDE.md), [`frontend/CLAUDE.md`](frontend/CLAUDE.md)에 정리되어 있습니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| 프론트엔드 | Next.js 16 (App Router), React 19, TypeScript, Redux Toolkit + RTK Query |
| 백엔드 | Java 25, Spring Boot 4, Spring Modulith (도메인 모듈 경계 강제), Spring Security |
| 영속성 | JPA + JDBC(`JdbcTemplate`) 혼용, CockroachDB (PostgreSQL 호환) |
| 실시간 | STOMP over WebSocket (메신저), Redis 기반 SSE (예약 대기열) |
| 결제 | 토스페이먼츠 (Payments Widget SDK + 서버 승인/취소 연동) |
| 인증 | 사내 중앙 인증(auth-core) 연동, mTLS, HashiCorp Vault (DB/Kafka 클라이언트 인증서 동적 발급) |
| CI/CD | GitHub Actions → GHCR → ArgoCD → Kubernetes (GitOps, 별도 매니페스트 저장소) |

## 스크린샷

<!-- TODO: 행사 목록, 결제 위젯, 매니저 체크인 스캐너, 정산 대시보드 화면 캡처 1~2장씩 추가 -->

## 담당 파트

결제(Payment) 도메인을 담당했습니다 — 토스페이먼츠 연동(`PaymentService`/`TossPaymentGateway`), 환불 정책(`RefundService`), 행사/광고 정산(`SettlementService`/`AdSettlementService`), 결제 로그 감사 추적을 구현했습니다.

## 로컬 실행

이 저장소는 사내 전용 Nexus/Vault/Kafka/CockroachDB 인프라(Cloudflare WARP Zero Trust로만 접근 가능)에 의존하므로, 이 공개 스냅샷만으로는 실행할 수 없습니다. 로컬 개발 환경 변수 목록은 [`.env.example`](.env.example), [`frontend/.env.example`](frontend/.env.example)을 참고하세요.
