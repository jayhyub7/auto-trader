package com.auto.trader.trade.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.auto.trader.domain.BaseEntity;
import com.auto.trader.domain.User;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.Side;
import com.auto.trader.trade.enums.OrderType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "executed_order", indexes = { @Index(name = "idx_executed_order_order_id", columnList = "orderId") })
public class ExecutedOrder extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String exchange; // BINANCE, BYBIT, etc.
	private String symbol; // BTCUSDT
	@Enumerated(EnumType.STRING)
	private Side side; // BUY / SELL
	private Direction direction; // LONG / SHORT

	private double quantity; // 체결 수량
	private double observedPrice; // 주문 전 사용자가 확인한 가격 (지표 기준 or 현재가)
	private double executedPrice; // 실제 체결된 가격
	private String orderId; // 거래소의 주문 ID

	private boolean success; // 주문 성공 여부

	@Column(name = "tp_sl_registered")
	private Boolean tpSlRegistered; // TP/SL 등록 성공 여부

	@Column(name = "execution_time_seconds")
	private Double executionTimeSeconds; // 주문 실행에 걸린 총 시간 (초 단위)

	@Column(columnDefinition = "TEXT")
	private String rawResponse; // 거래소에서 받은 응답 전체 (JSON 형태)

	private LocalDateTime executedAt; // 실제 체결 시각

	// 🔽 수수료 관련 필드
	@Column(name = "fee_amount")
	private Double feeAmount; // 수수료 수치

	@Column(name = "fee_currency")
	private String feeCurrency; // 수수료 통화 (예: USDT)

	@Column(name = "fee_rate")
	private Double feeRate; // 수수료 비율 (0.0004 등)

	@Enumerated(EnumType.STRING)
	private OrderType orderType; // 시장가, 지정가 등 주문 유형

	private Integer leverage; // 레버리지 배율

	@Column(name = "profit_percent")
	private Double profitPercent; // 수익률 (퍼센트)

	private boolean simulation; // 시뮬레이션 여부

	// ✅ [추가] 체결 당시 슬리피지 (백분율 단위로 저장됨)
	private Double slippage;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_open_id", nullable = false)
	private PositionOpen positionOpen; // 연관된 포지션 실행 정보

	@OneToMany(mappedBy = "executedOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ExecutedIndicator> indicators = new ArrayList<>(); // 체결 시점 지표 스냅샷

	@Column(columnDefinition = "TEXT")
	private String executionLog; // 조건 평가 전체 로그 (줄 단위 텍스트)

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
