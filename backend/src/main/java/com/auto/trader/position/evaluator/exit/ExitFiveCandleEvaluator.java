package com.auto.trader.position.evaluator.exit;

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
public class ExitFiveCandleEvaluator implements ExitConditionEvaluator {

	private static final double MIN_PROFIT_RATIO = 1.012; // 손익비 1.2 → 수익률 1.2배
	private static final int MIN_SEQUENCE = 5;

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction, double entryPrice,
			SchedulerLogManager logManager) {

		String key = "BTCUSDT_15m";
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();

		if (candles == null || candles.size() < MIN_SEQUENCE + 1) {
			logManager.log("[오캔들 종료] 캔들 부족");
			return false;
		}

		CandleDto current = candles.get(candles.size() - 1);

		// 5개 이상 연속 캔들 찾아 진입 기준선 복구
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
					break;
				}
				if (count >= MIN_SEQUENCE)
					break;
			}

			if (count < MIN_SEQUENCE)
				continue;

			CandleDto base = candles.get(start);
			double top = Math.max(base.getOpen(), base.getClose());
			double bottom = Math.min(base.getOpen(), base.getClose());

			// 익절 조건: 손익비 1.2 이상
			double profitTarget = direction == Direction.LONG ? entryPrice * MIN_PROFIT_RATIO
					: entryPrice / MIN_PROFIT_RATIO;

			if (direction == Direction.LONG && current.getHigh() >= profitTarget) {
				logManager.log("[오캔들 익절-LONG] 고가 {} ≥ 목표가 {} → 익절", current.getHigh(), profitTarget);
				return true;
			}
			if (direction == Direction.SHORT && current.getLow() <= profitTarget) {
				logManager.log("[오캔들 익절-SHORT] 저가 {} ≤ 목표가 {} → 익절", current.getLow(), profitTarget);
				return true;
			}

			// 손절 조건: 기준선 영역 돌파 시
			if (direction == Direction.LONG && current.getClose() < bottom) {
				logManager.log("[오캔들 손절-LONG] 종가 {} < 기준 하단 {} → 손절", current.getClose(), bottom);
				return true;
			}
			if (direction == Direction.SHORT && current.getClose() > top) {
				logManager.log("[오캔들 손절-SHORT] 종가 {} > 기준 상단 {} → 손절", current.getClose(), top);
				return true;
			}

			break;
		}

		logManager.log("[오캔들 종료] 익절/손절 조건 미충족");
		return false;
	}
}
