package com.auto.trader.position.dto;

import com.auto.trader.position.entity.IndicatorTypeEntity;
import com.auto.trader.position.enums.ConditionType;

public class IndicatorTypeDto {

	private String type;
	private String label;
	private ConditionType conditionType;

	public IndicatorTypeDto(String type, String label, ConditionType conditionType) {
		this.type = type;
		this.label = label;
		this.conditionType = conditionType;
	}

	public static IndicatorTypeDto from(IndicatorTypeEntity entity) {
		return new IndicatorTypeDto(entity.getType(), entity.getLabel(), entity.getConditionType());
	}

	public String getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public ConditionType getConditionType() {
		return conditionType;
	}
}
