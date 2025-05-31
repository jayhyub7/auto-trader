// 📁 com.auto.trader.trade.entity.TradeCondition.java

package com.auto.trader.trade.entity;

import com.auto.trader.domain.BaseEntity;
import com.auto.trader.position.enums.ConditionPhase;
import com.auto.trader.position.enums.IndicatorType;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.Timeframe;

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
@Table(name = "trade_condition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeCondition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trade_log_id", nullable = false)
	private TradeLog tradeLog;

	@Enumerated(EnumType.STRING)
	private IndicatorType type;

	private Double value;
	private Double k;
	private Double d;

	@Enumerated(EnumType.STRING)
	private Operator operator;

	@Enumerated(EnumType.STRING)
	private Timeframe timeframe;

	@Enumerated(EnumType.STRING)
	private ConditionPhase conditionPhase;

	private boolean enabled;

	@Override
	public String toString() {
		return String
			.format("📌 TradeCondition { type=%s, value=%.2f, k=%.2f, d=%.2f, operator=%s, timeframe=%s, phase=%s, enabled=%s }",
					type != null ? type.name() : "null", value != null ? value : 0.0, k != null ? k : 0.0,
					d != null ? d : 0.0, operator != null ? operator.name() : "null",
					timeframe != null ? timeframe.name() : "null",
					conditionPhase != null ? conditionPhase.name() : "null", enabled);
	}
}
