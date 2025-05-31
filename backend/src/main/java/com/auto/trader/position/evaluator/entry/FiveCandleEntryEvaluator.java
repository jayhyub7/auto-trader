package com.auto.trader.position.evaluator.entry;

import java.util.List;

import org.springframework.stereotype.Component;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FiveCandleEntryEvaluator implements EntryConditionEvaluator {

	private static final int MIN_SEQUENCE = 5; // 최소 연속 캔들 수

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _unused, Direction direction,
			SchedulerLogManager logManager) {

		String key = "BTCUSDT_15m";
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();

		if (candles == null || candles.size() < MIN_SEQUENCE + 1) {
			logManager.log("[오캔들] 캔들 부족: {}", candles != null ? candles.size() : 0);
			return false;
		}

		CandleDto current = candles.get(candles.size() - 1);

		// 뒤에서 앞으로 5개 이상 연속된 동일 방향 캔들 찾기
		for (int start = candles.size() - MIN_SEQUENCE - 1; start >= 0; start--) {
			int count = 0;
			boolean isUp = false;
			boolean isDown = false;

			for (int i = start; i < candles.size() - 1; i++) {
				CandleDto c = candles.get(i);
				if (c.getClose() > c.getOpen()) {
					if (count == 0)
						isUp = true;
					if (!isUp)
						break;
					count++;
				} else if (c.getClose() < c.getOpen()) {
					if (count == 0)
						isDown = true;
					if (!isDown)
						break;
					count++;
				} else {
					break; // 도지형 or 중립 캔들 → 시퀀스 끊김
				}
				if (count >= MIN_SEQUENCE)
					break;
			}

			if (count < MIN_SEQUENCE)
				continue;

			CandleDto baseCandle = candles.get(start); // 첫 번째 캔들
			double top = Math.max(baseCandle.getOpen(), baseCandle.getClose());
			double bottom = Math.min(baseCandle.getOpen(), baseCandle.getClose());

			// 진입 판단
			if (direction == Direction.LONG && isUp) {
				if (current.getLow() <= top && current.getLow() >= bottom) {
					logManager
						.log("[오캔들-LONG] 기준 영역 진입: top={}, bottom={}, current.low={}", top, bottom, current.getLow());
					return true;
				}
			}

			if (direction == Direction.SHORT && isDown) {
				if (current.getHigh() >= bottom && current.getHigh() <= top) {
					logManager
						.log("[오캔들-SHORT] 기준 영역 진입: top={}, bottom={}, current.high={}", top, bottom,
								current.getHigh());
					return true;
				}
			}

			break; // 하나만 감지하면 멈춤
		}

		logManager.log("[오캔들] 조건 불충족 (5개 이상 시퀀스 없음 또는 기준 영역 미접근)");
		return false;
	}
}
