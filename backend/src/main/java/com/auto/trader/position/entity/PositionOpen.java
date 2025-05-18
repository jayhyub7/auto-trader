package com.auto.trader.position.entity;

import java.time.LocalDateTime;

import com.auto.trader.domain.BaseEntity;
import com.auto.trader.position.enums.AmountType;
import com.auto.trader.position.enums.PositionOpenStatus;

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
@Table(name = "position_open")
public class PositionOpen extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int leverage;

	@Column(nullable = false)
	private double amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "amount_type", nullable = false)
	private AmountType amountType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PositionOpenStatus status;

	@Column(name = "stop_loss", nullable = false)
	private double stopLoss;

	@Column(name = "take_profit")
	private Double takeProfit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_id", nullable = false)
	private Position position;

	@Column(nullable = false)
	private boolean executed = false;

	private LocalDateTime executedAt;

	@Column(name = "current_order_id")
	private String currentOrderId;

	public boolean isValidStopLoss() {
		return stopLoss > 0 && !Double.isNaN(stopLoss) && !Double.isInfinite(stopLoss);
	}

	public boolean isValidTakeProfit() {
		return takeProfit != null && takeProfit > 0 && !takeProfit.isNaN() && !takeProfit.isInfinite();
	}
}
