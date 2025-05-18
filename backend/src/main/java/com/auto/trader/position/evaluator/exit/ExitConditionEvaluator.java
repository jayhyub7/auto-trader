// 📁 com.auto.trader.position.evaluator.exit

package com.auto.trader.position.evaluator.exit;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.indicator.IndicatorCache;

public interface ExitConditionEvaluator {
	boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log);
}

class ExitRsiEvaluator implements ExitConditionEvaluator {
	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log) {
		var rsiList = cache.getRsi();
		if (rsiList.isEmpty()) {
			log.log("⚠️ RSI 리스트 비어 있음");
			return false;
		}
		double current = rsiList.get(rsiList.size() - 1).getValue();
		double threshold = cond.getValue();
		Operator op = cond.getOperator();
		log.log("🧪 RSI 검사 | 현재: {}, 기준: {}, 연산자: {}", current, threshold, op);
		return op == Operator.이상 ? current >= threshold : current <= threshold;
	}
}

class ExitStochRsiEvaluator implements ExitConditionEvaluator {
	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log) {
		var list = cache.getStochRsi();
		if (list.isEmpty()) {
			log.log("⚠️ StochRSI 리스트 비어 있음");
			return false;
		}
		var latest = list.get(list.size() - 1);
		double k = latest.getK();
		double d = latest.getD();
		double threshold = cond.getValue();
		Operator op = cond.getOperator();

		log.log("🧪 StochRSI 검사 | K: {}, D: {}, 기준: {}, 연산자: {}", k, d, threshold, op);
		if (op == Operator.이상 && k < threshold || op == Operator.이하 && k > threshold) {
			log.log("❌ StochRSI 실패");
			return false;
		}

		if (cond.getK() != null && cond.getD() != null) {
			if (!(k > d && k - d >= 0.5)) {
				log.log("❌ 교차 조건 실패 (K={}, D={})", k, d);
				return false;
			}
		}

		return true;
	}
}

class ExitVwbbEvaluator implements ExitConditionEvaluator {
	private static final double VWBB_TOLERANCE_RATIO = 0.00005; // 0.005%

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, SchedulerLogManager log) {
		var vwbb = cache.getVwbb();
		if (vwbb.getBasis().isEmpty()) {
			log.log("⚠️ VWBB 기준선 없음");
			return false;
		}

		double current = cache.getCurrentPrice();
		double upper = vwbb.getUpper().getLast().getValue();
		double lower = vwbb.getLower().getLast().getValue();
		Operator op = cond.getOperator();

		double upperTolerance = upper * VWBB_TOLERANCE_RATIO;
		double lowerTolerance = lower * VWBB_TOLERANCE_RATIO;

		log.log("🧪 VWBB 검사 | 현재가: {}, 상단: {}, 하단: {}, 연산자: {}", current, upper, lower, op);

		switch (op) {
		case 상단_돌파 -> {
			if (current >= upper - upperTolerance) {
				log.log("✅ 상단 돌파 조건 통과 ({} ≥ {} - 허용오차 {})", current, upper, upperTolerance);
				return true;
			} else {
				log.log("❌ 상단 돌파 조건 실패 ({} < {} - 허용오차 {})", current, upper, upperTolerance);
				return false;
			}
		}
		case 하단_돌파 -> {
			if (current <= lower + lowerTolerance) {
				log.log("✅ 하단 돌파 조건 통과 ({} ≤ {} + 허용오차 {})", current, lower, lowerTolerance);
				return true;
			} else {
				log.log("❌ 하단 돌파 조건 실패 ({} > {} + 허용오차 {})", current, lower, lowerTolerance);
				return false;
			}
		}
		default -> {
			log.log("⚠️ 지원하지 않는 VWBB 연산자: {}", op);
			return false;
		}
		}
	}
}
