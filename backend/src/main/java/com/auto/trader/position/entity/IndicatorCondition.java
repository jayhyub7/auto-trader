package com.auto.trader.position.entity;

import com.auto.trader.domain.BaseEntity;
import com.auto.trader.position.enums.ConditionPhase;
import com.auto.trader.position.enums.IndicatorType;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.Timeframe;

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
public class IndicatorCondition extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private IndicatorType type;

	private Double value;
	private Double k;
	private Double d;

	@Enumerated(EnumType.STRING)
	private Operator operator;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private Timeframe timeframe;

	@Enumerated(EnumType.STRING) // ✅ 추가
	private ConditionPhase conditionPhase;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_id")
	private Position position;

	@Column(nullable = true)
	private Boolean enabled = true;
}
