
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
│   ├── PositionReport.tsx
│   ├── components/
│   └── services/
├── position-manager/
│   ├── PositionManager.tsx
│   ├── components/
│   └── services/
├── position-open/
│   ├── components/PositionOpen/
│   └── services/
├── futures/
│   ├── components/
│   └── services/

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

✅ zip 제공 원칙
- 신규 파일만 zip 압축하여 제공 (/src 기준 구조 유지)
- 기존 파일 수정분은 zip에 포함하지 않고 텍스트 출력
- 파일 이름, 위치, 구성 전부 프로젝트 구조에 맞게 유지

────────────────────────────────────────────
이 요약보고서는 전체 시스템 구조/흐름/연동/규칙/원칙을 모두 포함한 최신 기준 분석본입니다.
이 한 문서로 전체 이어서 작업 가능하며, 변경이 있을 경우 이 보고서만 갱신하면 됩니다.
