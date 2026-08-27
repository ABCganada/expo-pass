@AGENTS.md

# Last Mission Frontend Design System

> 이 문서는 **비주얼 디자인 가이드**다. 기능·구조·스택 규약의 기준은 `AGENTS.md` 이며,
> 충돌 시 항상 `AGENTS.md` 가 우선한다.
>
> **실제 스택** (디자인 정의를 구현하는 기반):
> - Next.js 16 (App Router) · React 19 · TypeScript
> - **순수 CSS Modules(`*.module.css`)** — SCSS/shadcn 미사용
> - 상태: **Redux Toolkit + RTK Query**(서버 상태), Redux slice(테마·소켓 등 전역)
> - 아이콘: **lucide-react**
> - 애니메이션: **CSS transition**(Framer Motion 미사용)

---

# Design Philosophy

Last Mission 은 박람회 예약·QR 입장 관리와 사내 인증·메신저를 제공하는 웹앱이다.

디자인 키워드
- Modern
- Premium
- Clean
- Spacious
- Professional
- Elegant
- Minimal

애플, Stripe Dashboard, Linear, Vercel Dashboard 스타일을 참고한다.

- 화려한 그라데이션을 남용하지 않는다.
- 여백을 충분히 사용한다.
- 카드 중심 레이아웃을 사용한다.
- Glassmorphism 사용 금지.
- Neumorphism 사용 금지.

---

# Design Tokens (색상·radius·shadow)

모든 디자인 토큰은 **CSS Custom Property** 로 등록하고, 컴포넌트 CSS Module 에서 `var(--xxx)` 로만 참조한다(하드코딩 금지).

토큰은 **테마 레이어**에 정의한다 — 다크모드를 지원하므로 라이트/다크 각각에서 같은 변수명을 재정의한다:
- `features/theme/themes/light.css`
- `features/theme/themes/dark.css`
- 전역 기본값·리셋은 `app/globals.css`

테마 전환은 루트의 `[data-theme]`(또는 테마 slice) 기준으로 토큰 값만 바뀌고 컴포넌트 코드는 그대로다.

## Color (의미 기반 토큰)

| 이름 | CSS 변수 | 용도 |
|---|---|---|
| Primary | `--color-primary` | 브랜드 아이덴티티 — 사이드바 선택 상태, 활성 인디케이터, 강조 |
| Primary Hover | `--color-primary-hover` | Primary 요소 hover |
| Page BG | `--color-page-background` | 페이지 최외곽 배경 |
| Content BG | `--color-content-background` | 본문 영역 배경 |
| Card BG | `--color-card-background` | 카드·모달 배경 |
| Sidebar BG | `--color-sidebar-background` | 사이드바 배경 |
| Border | `--color-border` | 구분선, 인풋 테두리 |
| Text | `--color-text-primary` | 본문 텍스트 |
| Sub Text | `--color-text-secondary` | 보조 텍스트, 캡션, placeholder |
| Danger | `--color-danger` | 에러, 삭제, 위험 액션 |
| Success | `--color-success` | 성공, 완료 상태 |

> 실제 팔레트 값(라이트/다크)은 `features/theme/themes/*.css` 를 단일 출처로 한다.
> 새 색이 필요하면 하드코딩하지 말고 두 테마 파일에 의미 기반 변수로 추가한다.

## Radius / Shadow

| 요소 | 변수(예) |
|---|---|
| Card | `--radius-card` |
| Button / Input | `--radius-button` / `--radius-input` |
| Dialog | `--radius-dialog` |
| Badge/Chip | `--radius-pill` (9999px) |

| 상태 | 변수(예) |
|---|---|
| Card | `--shadow-sm` |
| Hover | `--shadow-lg` |
| Dropdown/Modal | `--shadow-xl` |

radius·shadow 도 위 색상과 같은 방식으로 테마 레이어의 CSS 변수로 등록해 참조한다.

---

# Animation

- 모든 hover 전환은 150~200ms, `transition: ... 0.2s ease`.
- hover 시 `translateY(-2~4px)` 또는 `scale(1.02)` 정도. 과한 bounce 금지.
- 순수 CSS transition 으로 처리한다(Framer Motion 미사용).

```css
.card {
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
```

---

# Z-Index Scale

| 레이어 | z-index |
|---|---|
| Dropdown / Popover / Tooltip | 100 |
| Sticky Header | 200 |
| Sidebar (Mobile Drawer) | 300 |
| Modal / Dialog / Drawer / BottomSheet | 400 |
| Toast | 500 |

---

# Layout (Desktop 기준)

| 요소 | 값 |
|---|---|
| Sidebar | 280px |
| Top Header | 72px |
| Content max-width | 1600px |
| 페이지 padding | 32px |
| Card Gap | 24px |
| Section Gap | 40px |

---

# Typography

| 이름 | 크기 | Weight | Line-height |
|---|---|---|---|
| Title | 36px | 700 | 1.3 |
| Section | 28px | 700 | 1.35 |
| Card | 20px | 600 | 1.4 |
| Body | 16px | 400 | 1.6 |
| Caption | 14px | 400 | 1.5 |
| Label | 12px | 500 | 1.4 |

Font Family: `Inter`(기본), fallback `system-ui, -apple-system, sans-serif`. `app/globals.css` 의 `body` 에 전역 지정.

---

# Component Rules

- 모든 UI 는 컴포넌트화한다. 페이지(`app/**/page.tsx`)에서 직접 마크업을 남발하지 않고, feature 의 컴포넌트를 조합한다.
- 컴포넌트는 **직접 작성**한다(shadcn 등 외부 프리미티브 라이브러리 미사용).
- Button/Input 등 상호작용 컴포넌트는 `size` variant 지원.

| Size | Height |
|---|---|
| sm | 32px |
| md | 40px |
| lg | 48px |

Icon 사이즈 표준 (lucide-react)

| 컨텍스트 | 사이즈 |
|---|---|
| 사이드바 메뉴 아이콘 | 20px |
| 버튼 내부 아이콘 | 16px |
| 인풋 내부 아이콘 | 18px |
| Empty State 아이콘 | 48px |

---

# Component 세트

## 공용 프리미티브 (디자인 시스템)
Button(Primary/Secondary/Outline/Ghost/Icon/Danger, Loading 상태 지원), Input/Textarea/Select/Checkbox/Radio/Switch/SearchInput,
Card, Badge/Chip, Avatar, Tooltip/Popover/Dropdown, Tabs, Table+Pagination, Modal/ConfirmDialog/Drawer/BottomSheet, Toast/Alert,
EmptyState, Loading/Skeleton, Divider.

## 레이아웃/셸
Sidebar, Header, MobileBottomNav, Container, PageHeader, Section, Divider, Footer.

## 도메인 컴포넌트 (last-mission 실제 스코프)

아래 도메인은 전부 실제 API에 연결된 구현체다 — placeholder나 mock 데이터가 아니다.

- **auth**: LoginButton(중앙 인증 리다이렉트), 현재 사용자 표시
- **chat(메신저)**: MessengerWidget, RoomList, Conversation, OnlineUserList, ParticipantList, UserPicker, RoomActionDialog
- **event(행사)**: 행사 목록·상세, 카테고리, 티켓/콘텐츠/이미지 관리(매니저), 행사·티켓 관리(관리자)
- **reservation(예약)**: 예약 신청·내역·QR 티켓 발급, 대기열(Redis 기반 SSE), 매니저 QR 체크인 스캐너(`jsqr` 카메라 인식 + 수동 입력), 체크인 현황판
- **payment(결제/정산)**: `PaymentCheckoutButton`(토스페이먼츠 위젯 SDK 연동 실결제), `PaymentHistoryContent`/`PaymentDetailContent`(내 결제 내역), `ManagerSettlementListContent`(매니저 본인 행사 정산), `AdminPaymentDashboardContent`/`AdminPaymentLogListContent`/`AdminAdSettlementDashboardContent`(관리자 대시보드), XLSX 정산 리포트 내보내기
- **marketing(배너 광고)**: `VipAdsContent`/`BannerSlider`/`TabAdSection`(노출), `RegisterAdModal`/`AdCheckoutModal`(광고 등록·결제, 슬롯×일수 정가제), `AdminAdCard`(승인/반려), `MarketerAdCard`/`AdStatsModal`(노출·클릭·CTR 통계 + XLSX)
- **shell**: UserSidebar, MobileBottomNav, USER/MANAGER/ADMIN ModeSwitcher
- **admin**: AdminHeader, AdminSidebar, 행사별 예약 현황, 회원/권한 관리
- **theme**: ThemeToggle

---

# Sidebar Design

가장 중요한 컴포넌트.
- 배경: `--color-sidebar-background`
- Hover: 연한 Primary
- 선택된 메뉴: Primary 배경 + 왼쪽 4px 인디케이터, 아이콘도 Primary
- Hover 시 부드럽게 오른쪽 4px 이동, Chevron 은 hover 시에만
- 메뉴는 카드처럼 보이지 않게, Divider 적극 사용
- **Mobile/Tablet**: Drawer 로 전환 (아래 반응형 참고)

---

# Cards / Buttons

- Card: `--radius-card` + `--color-card-background` + `--shadow-sm`, hover 시 `translateY(-4px)` + `--shadow-lg`.
- Button: Primary(채움), Secondary(흰/서피스), Outline, Ghost, Icon(원형), Danger. 전 버튼 Loading 상태(스피너+disabled) 지원.

---

# Responsive Design

순수 CSS Modules 의 `@media` 로 처리한다(SCSS mixin 미사용). JS media query 훅은 인터랙션 로직(Drawer open/close 등)에만.

## Breakpoints

| 구분 | 범위 |
|---|---|
| Mobile | ~767px (기본, media query 없이) |
| Tablet | 768px ~ 1023px (`min-width: 768px`) |
| Desktop | 1024px ~ 1439px (`min-width: 1024px`) |
| Wide | 1440px+ (`min-width: 1440px`) |

```css
.metricGrid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
@media (min-width: 768px) { .metricGrid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; } }
@media (min-width: 1024px){ .metricGrid { grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 24px; } }
```

## Layout 변화
- **Sidebar**: Desktop/Wide(1024+) 고정 280px 노출 · Tablet 64px collapsed 또는 Drawer(기본 Drawer) · Mobile 숨김 → 햄버거로 Drawer(dim overlay, 바깥클릭/ESC 닫힘).
- **Header**: Desktop 72px 전체 노출 · Tablet 72px, 검색 아이콘 축소 · Mobile 56px, 햄버거+로고+필수 아이콘만.
- **Content**: Desktop/Wide max 1600px·padding 32px · Tablet padding 24px · Mobile padding 16px.
- **Grid(카드)**: Wide 4 / Desktop 3 / Tablet 2 / Mobile 1. Gap 24/·/20/16.
- **Table**: Desktop/Tablet 테이블(가로 스크롤 `overflow-x:auto`) · Mobile 카드 리스트로 전환.
- **Modal**: Desktop/Tablet Modal(중앙, max 480~640px) 또는 Drawer · Mobile BottomSheet 우선.
- **Typography(모바일 축소)**: Title 36→28 · Section 28→22 · Card 20→18 · Body 16→15 · Caption 14→13.

## 공통 규칙
- 반응형 분기는 CSS Module `@media` 로. 터치 타겟 최소 44×44px(Mobile/Tablet).
- 가로 스크롤은 Table 외에는 만들지 않는다.

---

# Accessibility
- `aria-label` 필수, button role 준수, `:focus-visible` 지원, tab navigation 지원, 터치 타겟 최소 44×44px(Mobile).

---

# Folder Structure (실제 구조)

```
app/                      # 라우트 진입점 + 레이아웃
  (user)/                 # 일반 사용자: 행사·예약·QR 티켓·결제내역·VIP 광고
  manager/                # 행사 관리자: 행사·티켓·예약자·체크인·정산·배너광고
  admin/                  # 전체 관리자: 행사·회원·결제/정산·배너 승인
  developer/              # 내부 옵저버빌리티(로그/메트릭/트레이스) — SaaS 도메인 밖
  login/
  globals.css
features/
  auth/                   # 인증 · 현재 사용자 상태 (authApi, useAuth, LoginButton)
  coderhan/               # 중앙 인증(auth-core) 로그인 버튼 등 사내 인증 연동 컴포넌트
  chat/                   # 메신저 UI · api · 모델
  event/                  # 행사·티켓·콘텐츠·이미지
  reservation/            # 예약·QR 티켓·대기열·체크인
  payment/                # 결제·환불·정산 (토스페이먼츠 연동)
  marketing/              # 배너 광고 등록·노출·통계
  shell/                  # 사용자 네비게이션 셸
  admin/                  # 관리자 네비게이션 셸
  socket/                 # STOMP 연결 지원
  store/                  # Redux 스토어 설정 (baseApi 등)
  theme/                  # 테마 상태·토글, themes/light.css · dark.css
  shared/                 # 크로스-피처 api·ui (styles/pageShell·panel 등)
```

- 컴포넌트는 각 feature 내부에 두고, 스타일은 **콜로케이션된 CSS Module**(`Component/Component.module.css` + `index.ts`).
- 페이지는 feature 컴포넌트를 조합만 하고, API/상태 로직을 중복 작성하지 않는다.
- `@/` import alias 사용.

---

# Coding Rules
- React Functional Component + TypeScript, Props Interface 작성, `forwardRef` 적극 사용.
- 컴포넌트 최대 250줄, 복잡하면 분리. Table 등 복합은 Compound Component 패턴.
- 스타일은 **CSS Modules(`*.module.css`)만**. 인라인/글로벌 CSS 남용 금지, 컴포넌트당 스타일 파일 1개.
- 색/radius/shadow 는 하드코딩 금지 — 반드시 `var(--xxx)` 참조(테마 토큰).
- 서버 상태는 RTK Query, 전역 클라이언트 상태는 Redux slice.

---

# Data Layer
- 서버 상태: **RTK Query** (`features/store` 의 baseApi 기반, 각 feature 의 `*Api.ts`).
- 전역 클라이언트 상태: **Redux Toolkit slice** (예: theme, socket).
- (TanStack Query·Zustand·React Hook Form 등은 사용하지 않는다.)

---

# Dark Mode
**지원한다.** `features/theme` 가 라이트/다크를 관리하며, `themes/light.css` · `themes/dark.css` 에서 **같은 CSS 변수명을 테마별로 재정의**한다. 컴포넌트는 `var(--xxx)` 만 쓰므로 테마 전환 시 코드 변경이 없다.

---

# Goal
모든 페이지는 컴포넌트를 조합해 만든다. 페이지에는 조합·구성만, UI 는 전부 feature 의 컴포넌트가 제공한다. 시각 규칙은 이 문서를, 기능·구조 규약은 `AGENTS.md` 를 따른다.
