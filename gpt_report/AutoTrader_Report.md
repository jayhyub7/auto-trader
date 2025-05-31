📑 AutoTrader 시스템 전체 구조 요약보고서 (2025-05-30 / GPT-4o 기준, 최신 소스 기반 완전 분석)

────────────────────────────────────────────

1. ⚙️ 백엔드 구조 (Spring Boot + Gradle)
   ────────────────────────────────────────────

📁 도메인 패키지 구조

- com.auto.trader.adminkey: 관리자 인증키 설정
- com.auto.trader.balance: 잔고 조회 및 캐싱
- com.auto.trader.exchange: 거래소 인터페이스 및 Binance/Bybit 구현
- com.auto.trader.indicator: 지표 계산 및 비교
- com.auto.trader.position: 포지션/조건 관리 및 실행 상태
- com.auto.trader.trade: 체결 정보, 전략 평가, 체결 로그 저장
- com.auto.trader.scheduler: ENTRY/EXIT/INDICATOR 등 스케줄러 및 실행 상태 제어

📦 주요 Entity/서비스 흐름

- ExecutedOrder: 체결 정보 저장 (수익률, 레버리지, 슬리피지 포함)
- ExecutedIndicator: 체결 시점의 지표들 (RSI, Stoch, VWBB)
- TradeLog + TradeCondition: 체결 당시 포지션/조건 스냅샷 (회고용)
- Position + PositionOpen: 포지션 조건, 방향, 사용 여부, 시뮬레이션 여부 등
- 모든 지표는 IndicatorMemoryStore에 분봉별 캐시됨
- 주문 시 TP/SL 설정 포함 (시장가 주문 후 조건부 등록)

🧠 스케줄러 흐름

- EntryTradeScheduler: 진입 조건 1초 단위 평가 → 주문 실행
- ExitTradeScheduler: 종료 조건/StopLoss 감지 → 시장가 종료
- IndicatorScheduler: WebSocket + 실시간 분봉 지표 캐시 유지
- BalanceScheduler: 주기적 잔고 조회 및 캐싱
- FxScheduler: 환율 정보 저장

────────────────────────────────────────────

2. 🧩 프론트엔드 구조 (React + Vite + TypeScript)
   ────────────────────────────────────────────

📁 /src 구조

features/
├── executed-report/
│ ├── PositionReport.tsx
│ ├── components/
│ └── services/
├── position-manager/
│ ├── PositionManager.tsx
│ ├── components/
│ └── services/
├── position-open/
│ ├── components/PositionOpen/
│ └── services/
├── futures/
│ ├── components/
│ └── services/

pages/
├── futures/ExecutedReportPage.tsx
├── admin/...

service/
├── AdminKeyService.ts
├── balanceService.ts
├── IndicatorComparisonService.ts
├── SchedulerToggleManagerService.ts

기타 공통: assets/, constants/, hooks/, lib/, shared/, components/

🖥 주요 화면 컴포넌트

- PositionManager.tsx: 포지션 조건 추가/삭제, 사용 여부
- PositionOpen: 포지션 실행/취소 상태 관리
- ExecutedReport (PositionReport): 체결 내역, 수익률, 조건, 로그
- BitcoinChart.tsx: 실시간 WebSocket 기반 차트
- IndicatorComparison.tsx: 지표 결과 오차 시각화
- Sidebar.tsx: 전체 메뉴 진입점 (openTab 구조 기반)

🔗 화면 전환 구조

- Sidebar → openTab(id, label) 호출
- 상위 컴포넌트에서 id별로 컴포넌트 매핑 (ex: executed-report → <PositionReport />)
- Sidebar에서 메뉴 항목 추가만으로 진입 가능

────────────────────────────────────────────

3. 📌 시스템 설계 및 구현 원칙
   ────────────────────────────────────────────

💾 저장 및 평가

- saveAll() 사용 금지 → 개별 save() 방식 고정
- position.direction만 사용, 조건마다 direction 없음
- 전략 평가는 ConditionEvaluator 전략 패턴으로 분리
- 모든 조건 평가 로그는 ExecutedOrder.executionLog에 저장
- 체결 시점 기준 포지션과 조건은 TradeLog + TradeCondition으로 스냅샷 저장

📡 지표/WebSocket

- 모든 지표는 frontend/backend 동일 로직
- WebSocket은 단일 연결 + 다중 stream 처리
- 1분봉 기준 정확한 정각 timestamp 처리 (now - now % 60000)
- IndicatorMemoryStore는 시간별 구분 (1m, 3m, 5m 등)

📁 프론트 구조/원칙

- 기능 단위 분리: features/position-manager, features/executed-report 등
- 각 기능은 components/, services/, 메인 컴포넌트 파일(tsx) 구성
- pages/는 라우팅/탭 기반 entry 구성
- API 통신은 /service 또는 기능별 services/ 내에 위치
- 파일/폴더명은 kebab-case 또는 lowercase
- 타입은 API 파일 내부에서 함께 정의

────────────────────────────────────────────

4. 🧷 절대 행동 원칙 (출력/수정/분석 시 반드시 지켜야 할 사항)
   ────────────────────────────────────────────

✅ 전체 파일 출력 원칙

- 기존 파일을 수정할 경우 → 반드시 import 포함 전체 파일로 출력
- 함수 단위 요청 시에도 전체 함수 출력, 파일명+라인 주석 포함
- "변경할 위치 알려줘" 요청 시에도 전체 반영이 기본

✅ 분석/구조 유지 원칙

- 소스 분석 시 절대로 이전 기억에 의존하지 않음 → 항상 zip 분석으로 처음부터 재확인
- 구조, 파일 위치, import 경로는 실제 존재 경로 기준으로 확인 후 반영
- 경로/이름/구조 자동 정합성 검증 포함
- "다시 분석하라고 하면 반드시 처음부터 다시 분석" 원칙 고정
- 기억이 불확실할 경우 반드시 사용자에게 확인 요청
- 구조 판단 기준 파일: AutoTrader_Report_2025-05-30_LATEST.txt (명시적 지시 없이는 변경 불가)

✅ zip 제공 원칙

- 신규 파일만 zip 압축하여 제공 (/src 기준 구조 유지)
- 기존 파일 수정분은 zip에 포함하지 않고 텍스트 출력
- 파일 이름, 위치, 구성 전부 프로젝트 구조에 맞게 유지

────────────────────────────────────────────

5. 🧭 개발 진행 흐름 요약 (2025년 5월 기준)
   ────────────────────────────────────────────

1단계: 포지션 및 조건 관리 화면 구축

- PositionManager.tsx, IndicatorCondition 구조 설계
- LONG/SHORT 방향 설정 및 조건별 사용 여부 관리

2단계: 자동매매 구조 설계 (Entry/ExitTradeScheduler 분리)

- Entry는 진입 조건만, Exit은 종료조건 + StopLoss
- 전략 패턴 도입 및 조건 평가 구조 분리

3단계: 시장가 주문 체결 및 TP/SL 연동 구현

- TP/SL 계산 및 거래소별 등록 (`placeStopLossOrder`, `placeTakeProfitOrder`)
- TP/SL 등록 성공 여부 기록

4단계: 체결 정보 저장 구조 확립

- ExecutedOrder: 수익률, 체결가, 시장가, 레버리지 등
- ExecutedIndicator: 체결 시점 다중 분봉 지표(RSI, StochRSI, VWBB) 저장

5단계: 프론트 실행 구조 리팩토링

- PositionCard.tsx 구조 정비 및 실행/시뮬레이션/취소 버튼 통합
- openData 상태 관리 개선

6단계: 실시간 차트 및 지표 비교 구조 구현

- BitcoinChart.tsx: 1분봉 WebSocket + 지표 실시간 렌더링
- IndicatorComparison.tsx: frontend/backend 차이 시각화

7단계: 백엔드 지표 캐시 구조 개선

- IndicatorScheduler: 1초 주기 분봉 단위 지표 WebSocket 처리
- IndicatorMemoryStore: 분봉별 데이터 캐싱 (1m, 3m, 5m, 15m, 1h, 4h 등)

8단계 (진행 중): 수익률 통계 기능 개발

- 체결 정보 기반 수익률/TP 등록 여부/시장가 차이 등 통계화
- ExecutedReport 화면 정비 및 통계 집계 API 설계 예정

────────────────────────────────────────────

6. 🧾 체결 로그 구조 (ExecutedOrder 기준)
   ────────────────────────────────────────────

체결 시점에 EntryTradeScheduler 및 ExitTradeScheduler에서는 총 3가지 종류의 로그 및 정보를 기록하며, 각기 다른 위치에 저장된다:

1️⃣ 조건 평가 로그

- 저장 위치: `ExecutedOrder.executionLog` (텍스트 문자열)
- 평가에 사용된 지표 값과 기준값 비교 내역

2️⃣ 체결 메타정보 로그

- 저장 위치: `ExecutedOrder.executionLog` (같은 필드에 이어서 포함)
- 체결 전 시장가, 실제 체결가, 슬리피지, TP/SL 등록 여부 등

3️⃣ 전략 스냅샷 로그

- 저장 위치: `TradeLog`, `TradeCondition`
- 체결 당시 포지션 조건 구성을 스냅샷 형태로 기록
- 조건 구성 변경 추적 및 회고 기능에 활용됨

📦 추가로, 지표 값 스냅샷은 `ExecutedIndicator` 엔티티에 저장되며, 체결 당시 RSI, StochRSI, VWBB 등의 다중 분봉 기준 지표가 포함된다.

모든 체결 로그는 `ExecutedOrderService.saveExecutedOrderWithIndicators(...)`를 통해 통합적으로 저장되며, 실행된 포지션과 조건, 방향, 거래소, 타임스탬프 등은 공통 파라미터로 관리된다.

────────────────────────────────────────────

6. 사용 중인 매매 전략 개요
   ────────────────────────────────────────────

📌 1. 스탑헌팅 매매법

- **개념**: 세력이 고의로 개미들의 손절(스탑로스)을 유도한 후 되돌림을 통해 다시 원래 방향으로 진입하는 "헌팅 패턴"을 역이용한 전략
- **진입 기준**: 고점/저점 돌파 후 꼬리 발생 → 되돌림 → 진입
- **롱/숏 조건**:

  - 롱: 저점 스탑헌팅 후 반등 시 진입
  - 숏: 고점 스탑헌팅 후 하락 시 진입

- **손절**: 꼬리 상단 or 하단 이탈 시
- **익절**: 손익비 ≥ 1.2 이상부터 자율 익절
- **주의사항**:

  - 진입은 되돌림 캔들 확인 후
  - 도지형/꼬리 과도한 캔들 제외

📌 2. 오캔들 매매법

- **개념**: 동일 방향의 양봉 or 음봉이 5개 이상 연속 발생 시, 첫 번째 캔들의 몸통 영역을 지지/저항으로 삼아 해당 영역에 되돌아올 경우 진입하는 전략
- **진입 조건**: 5개 이상 연속 양봉/음봉 + 현재가가 첫 번째 캔들의 몸통 영역에 진입
- **손절**: 기준 몸통 영역의 종가 돌파 시
- **익절**: 손익비 ≥ 1.2 이상부터 자율 익절
- **주의사항**:

  - 3\~4캔들 구간 무효
  - 몸통 기준(꼬리 제외)으로 지지/저항 판단
  - 진입은 터치 시점 기준, 복귀 캔들 확인 필요 없음 (단순 진입 허용)

────────────────────────────────────────────

7. 🧪 병합 테스트 전략 구성 (`TestEvaluator` 기준)
   ────────────────────────────────────────────

- `TestEvaluator`는 실전 전략 병합 실험을 위한 evaluator로, 다음 전략들을 동시에 평가함:

✅ **스탑헌팅 1분봉 버전**

- 최근 고점/저점 돌파 + wick 존재 + 복귀 확인 시 진입
- 15분봉 전략과 동일한 구조를 `BTCUSDT_1m` 기준으로 구현함

✅ **오캔들 1분봉 버전**

- 5개 이상 연속 캔들 출현 후, 첫 캔들 몸통 영역에 진입 시 판단

- 기준선이 아닌 영역 기반, 꼬리 제외 구조 적용

- 이 evaluator는 `evaluateStopHunting(...)`, `evaluateFiveCandle(...)`로 메서드 분리되어 있으며, 향후 전략 추가 및 확장이 용이함

- 실험적 적용이므로 체결 후 전략명이 `ExecutedOrder` 등에는 저장되지 않음 (단순 평가용으로만 사용됨)
