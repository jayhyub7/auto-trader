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
public class ExitStopHuntingEvaluator implements ExitConditionEvaluator {

	private static final double ENTRY_STOP_RATIO = 0.01; // 손절 -1% 기준
	private static final double MIN_PROFIT_MULTIPLIER = 1.2; // 손익비 1.2 이상 익절

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction, double entryPrice,
			SchedulerLogManager logManager) {
		String key = "BTCUSDT_15m";
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();

		if (candles == null || candles.size() < 3) {
			logManager.log("⚠️ [스탑헌팅 종료] 캔들 부족");
			return false;
		}

		CandleDto wickCandle = candles.get(candles.size() - 2); // 진입 직전 봉
		CandleDto current = candles.get(candles.size() - 1);

		double stopRange = entryPrice * ENTRY_STOP_RATIO;
		double profitTarget = direction == Direction.LONG ? entryPrice + stopRange * MIN_PROFIT_MULTIPLIER
				: entryPrice - stopRange * MIN_PROFIT_MULTIPLIER;

		// ✅ 익절 조건
		if (direction == Direction.LONG && current.getHigh() >= profitTarget) {
			logManager.log("[익절-LONG] 목표가 도달: 현재 고가 {} ≥ 목표가 {}", current.getHigh(), profitTarget);
			return true;
		}
		if (direction == Direction.SHORT && current.getLow() <= profitTarget) {
			logManager.log("[익절-SHORT] 목표가 도달: 현재 저가 {} ≤ 목표가 {}", current.getLow(), profitTarget);
			return true;
		}

		// ✅ 손절 조건 (꼬리 이탈)
		if (direction == Direction.LONG && current.getClose() < wickCandle.getLow()) {
			logManager.log("[손절-LONG] 종가 < wick 하단 → 손절: {} < {}", current.getClose(), wickCandle.getLow());
			return true;
		}
		if (direction == Direction.SHORT && current.getClose() > wickCandle.getHigh()) {
			logManager.log("[손절-SHORT] 종가 > wick 상단 → 손절: {} > {}", current.getClose(), wickCandle.getHigh());
			return true;
		}

		logManager.log("[스탑헌팅 종료] 익절/손절 조건 미충족 (유지 중)");
		return false;
	}
}
