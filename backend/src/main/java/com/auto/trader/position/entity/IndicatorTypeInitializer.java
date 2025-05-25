package com.auto.trader.position.entity;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.auto.trader.position.enums.ConditionType;
import com.auto.trader.position.repository.IndicatorTypeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IndicatorTypeInitializer implements CommandLineRunner {

	private final IndicatorTypeRepository repository;

	@Override
	public void run(String... args) {
		if (repository.count() == 0) {
			repository
				.saveAll(List
					.of(new IndicatorTypeEntity(null, "RSI", "RSI", ConditionType.INDICATOR),
							new IndicatorTypeEntity(null, "VWBB", "VWBB", ConditionType.INDICATOR),
							new IndicatorTypeEntity(null, "STOCH_RSI", "STOCH RSI", ConditionType.INDICATOR),
							new IndicatorTypeEntity(null, "STOP_HUNTING", "스탑헌팅", ConditionType.STRATEGY),
							new IndicatorTypeEntity(null, "STOP_HUNTING_1M", "스탑헌팅_1M", ConditionType.STRATEGY),
							new IndicatorTypeEntity(null, "FIVE_CANDLE", "5캔들", ConditionType.STRATEGY),
							new IndicatorTypeEntity(null, "TEST", "테스트", ConditionType.STRATEGY)));

		}
	}
}