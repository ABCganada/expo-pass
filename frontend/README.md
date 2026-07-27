# Last Mission Frontend

Last Mission의 사용자·관리자 웹 화면을 제공하는 Next.js 프런트엔드입니다.

## 현재 범위

- 공통 인증 연동 및 로그인 사용자 정보 표시
- 사용자 홈
- 실시간 접속 상태와 메신저
- 관리자 레이아웃
- 회원 관리 자리표시 화면 (`업데이트 예정입니다.`)

시험, 응시, 신청, 학습 관리, 오답 노트 기능은 템플릿에서 제거했습니다.

## 기술 구성

| 영역 | 기술 |
|---|---|
| 프레임워크 | Next.js App Router |
| 상태 관리 | Redux Toolkit, RTK Query |
| UI | React, TypeScript, CSS Modules |
| 실시간 통신 | STOMP WebSocket |
| 런타임 | Node.js 24 |

## 프로젝트 구조

```text
app/
├─ (user)/                 사용자 홈과 공통 셸
├─ admin/                  관리자 셸과 회원 관리 자리표시 화면
└─ login/                  로그인 화면
features/
├─ auth/                   현재 사용자와 공통 인증 연동
├─ chat/                   메신저
├─ shared/                 공용 API·UI
└─ theme/                  테마 설정
k8s/                       프런트 Deployment·Service·Ingress
.github/workflows/         이미지 빌드와 매니페스트 갱신
```

## 로컬 실행

```bash
npm ci
npm run dev
```

로컬 개발값은 Git에서 제외되는 `.env.development.local`에 둡니다.

```dotenv
NEXT_PUBLIC_API_URL=https://<backend-host>
```

`NEXT_PUBLIC_*` 값은 브라우저에 공개되고 빌드 결과에 포함되므로 비밀번호, 토큰, 인증서 경로와 Secret을 넣지 않습니다.

## 검증

```bash
npm run lint
npm run build
```

`main` 브랜치에 애플리케이션 코드가 반영되면 GitHub Actions가 GHCR 이미지를 만들고 `k8s/lastmission-frontend.yaml`의 이미지 태그를 갱신합니다.
