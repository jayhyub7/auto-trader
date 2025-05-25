package com.auto.trader.position.evaluator.exit;

import java.util.List;

import org.springframework.stereotype.Component;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil.DualIndicatorPoint;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExitStopHuntingEvaluator implements ExitConditionEvaluator {

	private static final double PROFIT_RATIO = 0.01; // 1% 익절 기준
	private static final double STOCH_RSI_MULTIPLIER = 1.3;

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction,
			SchedulerLogManager logManager) {
		String key = "BTCUSDT_15m";

		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();
		if (candles == null || candles.size() < 3) {
			logManager.log("[스탑헌팅 종료] 캔들 부족");
			return false;
		}

		CandleDto wickCandle = candles.get(candles.size() - 2);
		CandleDto current = candles.get(candles.size() - 1);

		double entryPrice = wickCandle.getOpen();
		double profitTarget = direction == Direction.LONG ? entryPrice + entryPrice * PROFIT_RATIO
				: entryPrice - entryPrice * PROFIT_RATIO;

		if (direction == Direction.LONG && current.getHigh() >= profitTarget) {
			logManager.log("[스탑헌팅 익절 - 롱] 목표가 도달: {} ≥ {} → 익절", current.getHigh(), profitTarget);
			return true;
		}

		if (direction == Direction.SHORT && current.getLow() <= profitTarget) {
			logManager.log("[스탑헌팅 익절 - 숏] 목표가 도달: {} ≤ {} → 익절", current.getLow(), profitTarget);
			return true;
		}

		// StochRSI 기반 익절 조건
		List<DualIndicatorPoint> stochList = cache.getStochRsi();
		if (stochList != null && stochList.size() >= 3) {
			DualIndicatorPoint currentStoch = stochList.get(stochList.size() - 1);
			double sum = 0;
			int count = 0;
			for (int i = Math.max(0, stochList.size() - 500 - 1); i < stochList.size() - 1; i++) {
				DualIndicatorPoint a = stochList.get(i);
				DualIndicatorPoint b = stochList.get(i + 1);
				if (a.getK() != null && b.getK() != null && a.getK() < 20 && b.getK() > a.getK()) {
					sum += a.getK();
					count++;
				}
			}
			double avgBounce = (count > 0) ? (sum / count) : 10.0;
			double threshold = avgBounce * STOCH_RSI_MULTIPLIER;

			if (currentStoch.getK() != null && currentStoch.getK() >= threshold) {
				logManager.log("[스탑헌팅 익절 - StochRSI] 현재 K={} ≥ 임계치 {} → 익절", currentStoch.getK(), threshold);
				return true;
			}
		}

		// 손절 조건
		if (direction == Direction.LONG && current.getClose() < wickCandle.getLow()) {
			logManager.log("[스탑헌팅 종료 - 롱] 종가 < wickLow → 종료 조건 충족 ({} < {})", current.getClose(), wickCandle.getLow());
			return true;
		}

		if (direction == Direction.SHORT && current.getClose() > wickCandle.getHigh()) {
			logManager
				.log("[스탑헌팅 종료 - 숏] 종가 > wickHigh → 종료 조건 충족 ({} > {})", current.getClose(), wickCandle.getHigh());
			return true;
		}

		logManager.log("[스탑헌팅 종료] wick 자리 유지 중, 익절/손절 조건 미충족");
		return false;
	}
}