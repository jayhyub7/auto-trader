package com.auto.trader.trade.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.auto.trader.domain.BaseEntity;
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
import jakarta.persistence.Lob;
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
	private Side side; // BUY / SELL
	private Direction direction; // Long / Short
	private double quantity;
	private double observedPrice; // 주문 전 사용자가 확인한 가격 (지표 계산 or 현재가)
	private double executedPrice; // 실제 체결된 가격 (result.getPrice())
	private String orderId; // 거래소의 주문 ID

	private boolean success;

	@Column(name = "tp_sl_registered")
	private Boolean tpSlRegistered; // TP/SL 등록 성공 여부

	@Column(name = "execution_time_seconds")
	private Double executionTimeSeconds; // 전체 실행 소요 시간

	@Lob
	private String rawResponse; // 거래소 응답 전체 (JSON String)

	private LocalDateTime executedAt;

	// 🔽 수수료 관련 추가 필드
	// 🔁 통합된 수수료 필드
	@Column(name = "fee_amount")
	private Double feeAmount;

	@Column(name = "fee_currency")
	private String feeCurrency;

	@Column(name = "fee_rate")
	private Double feeRate;

	@Enumerated(EnumType.STRING)
	private OrderType orderType;

	private Integer leverage;

	@Column(name = "profit_percent")
	private Double profitPercent;

	private boolean simulation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_open_id", nullable = false)
	private PositionOpen positionOpen;

	@OneToMany(mappedBy = "executedOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ExecutedIndicator> indicators = new ArrayList<>();

	@Column(columnDefinition = "TEXT")
	private String executionLog; // 조건 평가 전체 로그 (줄 단위 텍스트)
}
