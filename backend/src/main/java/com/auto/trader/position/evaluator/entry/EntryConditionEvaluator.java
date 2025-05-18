package com.auto.trader.position.evaluator.entry;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.indicator.IndicatorCache;

public interface EntryConditionEvaluator {
	boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log);
}

class RsiEvaluator implements EntryConditionEvaluator {
	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log) {
		var rsiList = cache.getRsi();
		if (rsiList.isEmpty()) {
			log.log("⚠️ RSI 리스트 비어 있음");
			return false;
		}

		double value = cond.getValue();
		double currentRsi = rsiList.get(rsiList.size() - 1).getValue();
		log.log("📈 [RSI 검사] 현재: {}, 기준: {}, 연산자: {}", currentRsi, value, cond.getOperator());

		if (cond.getOperator() == Operator.이상) {
			if (currentRsi < value) {
				log.log("❌ RSI 조건 실패: {} < {}", currentRsi, value);
				return false;
			}
		} else if (cond.getOperator() == Operator.이하) {
			if (currentRsi > value) {
				log.log("❌ RSI 조건 실패: {} > {}", currentRsi, value);
				return false;
			}
		}

		log.log("✅ RSI 조건 통과");
		return true;
	}
}

class StochRsiEvaluator implements EntryConditionEvaluator {
	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log) {
		var stochList = cache.getStochRsi();
		if (stochList.isEmpty()) {
			log.log("⚠️ StochRSI 리스트 비어 있음");
			return false;
		}

		double value = cond.getValue();
		Double kTarget = cond.getK();
		Double dTarget = cond.getD();

		var latest = stochList.get(stochList.size() - 1);
		double currentK = latest.getK();
		double currentD = latest.getD();

		log.log("📉 [StochRSI 검사] K: {}, D: {}, 기준: {}, 연산자: {}", currentK, currentD, value, cond.getOperator());

		if (cond.getOperator() == Operator.이상 && currentK < value) {
			log.log("❌ K값 조건 실패: {} < {}", currentK, value);
			return false;
		} else if (cond.getOperator() == Operator.이하 && currentK > value) {
			log.log("❌ K값 조건 실패: {} > {}", currentK, value);
			return false;
		}

		if (kTarget != null && dTarget != null) {
			if (currentK > currentD && currentK - currentD >= 0.5) {
				log.log("✅ 교차 조건 통과 (%K > %D)");
			} else {
				log.log("❌ 교차 조건 실패 (%K={}, %D={}, 차이={})", currentK, currentD, currentK - currentD);
				return false;
			}
		}

		log.log("✅ K값 조건 통과");
		return true;
	}
}

class VwbbEvaluator implements EntryConditionEvaluator {
	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log) {
		var basis = cache.getVwbb().getBasis();
		var upper = cache.getVwbb().getUpper();
		var lower = cache.getVwbb().getLower();
		double currentPrice = cache.getCurrentPrice();

		if (basis.isEmpty()) {
			log.log("⚠️ VWBB 기준선 없음");
			return false;
		}

		int size = basis.size();
		double upperBand = upper.get(size - 1).getValue();
		double lowerBand = lower.get(size - 1).getValue();
		double basisVal = basis.get(size - 1).getValue();
		long lastCandleTime = cache.getCandles().get(cache.getCandles().size() - 1).getTime();

		log
			.log("📊 [VWBB 검사] 현재가: {}, 상단: {}, 기준선: {}, 하단: {}, 캔들 수: {}, 마지막 캔들 UTC: {}", currentPrice, upperBand,
					basisVal, lowerBand, cache.getCandles().size(), lastCandleTime);

		if (cond.getOperator() == Operator.상단_돌파) {
			if (currentPrice > upperBand) {
				log.log("✅ 상단 돌파 조건 통과 ({} > {})", currentPrice, upperBand);
				return true;
			} else {
				log.log("❌ 상단 돌파 조건 실패 ({} <= {})", currentPrice, upperBand);
				return false;
			}
		}

		if (cond.getOperator() == Operator.하단_돌파) {
			if (currentPrice < lowerBand) {
				log.log("✅ 하단 돌파 조건 통과 ({} < {})", currentPrice, lowerBand);
				return true;
			} else {
				log.log("❌ 하단 돌파 조건 실패 ({} >= {})", currentPrice, lowerBand);
				return false;
			}
		}

		log.log("⚠️ VWBB 연산자 일치 없음: {}", cond.getOperator());
		return false;
	}

}
