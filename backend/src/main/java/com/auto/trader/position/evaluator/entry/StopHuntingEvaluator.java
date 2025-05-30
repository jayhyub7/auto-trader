package com.auto.trader.position.evaluator.entry;

import java.util.List;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil.DualIndicatorPoint;

/**
 * 항목 판단 의미 있는 조건 고점/저점 돌파 + 꼬리 필터는 실제 스탑헌팅 패턴에 근접 ✅ 리스크 필터링 도지 캔들 제거, 꼬리 비율 제한
 * 등 ✅ 후행성 낮음 (직전 2개 캔들만 평가) ✅ 거짓 신호 좁은 박스장에서 빈번하게 발생할 수 있음 ⚠️ 전략 보완 확인봉 body 조건
 * 추가, 볼륨 필터, RSI 등 보완 시 매우 유효 ✅ ✔️ 스탑헌팅 전략으로 실전성 충분히 있음 단독 사용 시 오탐이 꽤 있으나, 추가
 * 지표 필터링(예: StochRSI, VWBB 중단선 이탈 여부)**와 함께 쓰면 매우 강력한 진입 전략으로 활용 가능함.
 */
public class StopHuntingEvaluator implements EntryConditionEvaluator {

	private static final int MIN_CANDLES = 50;
	private static final double MAX_WICK_RATIO = 4.0; // wick이 body의 4배 초과면 제외
	private static final int STOCH_LOOKBACK = 500;
	private static final double STOCH_BOUNCE_THRESHOLD_MULTIPLIER = 2;

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction,
			SchedulerLogManager log) {
		String key = "BTCUSDT_15m";

		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();

		if (candles == null || candles.size() < MIN_CANDLES) {
			log.log("⚠️ 캔들 개수 부족 ({}개)", candles != null ? candles.size() : 0);
			return false;
		}

		CandleDto wickCandle = candles.get(candles.size() - 2); // 스탑헌팅 발생 봉
		CandleDto confirmCandle = candles.get(candles.size() - 1); // 진입 확인 봉

		double recentHigh = candles
			.stream()
			.skip(candles.size() - MIN_CANDLES)
			.mapToDouble(CandleDto::getHigh)
			.max()
			.orElse(Double.NaN);

		double recentLow = candles
			.stream()
			.skip(candles.size() - MIN_CANDLES)
			.mapToDouble(CandleDto::getLow)
			.min()
			.orElse(Double.NaN);

		log.log("📊 최근 고점: {}, 저점: {}", (int) recentHigh, (int) recentLow);

		boolean stopTriggered = false;
		if (direction == Direction.SHORT && wickCandle.getHigh() >= recentHigh) {
			stopTriggered = true;
		} else if (direction == Direction.LONG && wickCandle.getLow() <= recentLow) {
			stopTriggered = true;
		}

		if (!stopTriggered) {
			log.log("❌ 스탑헌팅 기준 고/저점 터치 안됨");
			return false;
		}

		// ⛔ wick이 너무 긴 경우 필터링
		double wickBody = Math.abs(wickCandle.getClose() - wickCandle.getOpen());
		if (wickBody < 1e-8) {
			log.log("❌ wickBody=0 도지형 캔들 → 무시");
			return false;
		}
		double upperWick = wickCandle.getHigh() - Math.max(wickCandle.getClose(), wickCandle.getOpen());
		double lowerWick = Math.min(wickCandle.getClose(), wickCandle.getOpen()) - wickCandle.getLow();

		if (direction == Direction.SHORT && upperWick > wickBody * MAX_WICK_RATIO) {
			log.log("❌ 윗꼬리 비율 과다 → 무시 (upperWick: {}, body: {})", (int) upperWick, (int) wickBody);
			return false;
		}
		if (direction == Direction.LONG && lowerWick > wickBody * MAX_WICK_RATIO) {
			log.log("❌ 아랫꼬리 비율 과다 → 무시 (lowerWick: {}, body: {})", (int) lowerWick, (int) wickBody);
			return false;
		}

		// ✅ 진입 확인 캔들: 반전 캔들로 마감되었는가?
		boolean confirmation = false;
		if (direction == Direction.SHORT && confirmCandle.getClose() < confirmCandle.getOpen()
				&& confirmCandle.getClose() < wickCandle.getOpen()) {
			confirmation = true;
		} else if (direction == Direction.LONG && confirmCandle.getClose() > confirmCandle.getOpen()
				&& confirmCandle.getClose() > wickCandle.getOpen()) {
			confirmation = true;
		}

		if (!confirmation) {
			log.log("❌ 진입 확인 캔들 미약함 또는 반전 불충분");
			return false;
		}

		// ✅ Stoch RSI 필터 추가 (DualIndicatorPoint 기준)
		List<DualIndicatorPoint> stochList = cache.getStochRsi();
		if (stochList == null || stochList.size() < 3) {
			log.log("⚠️ StochRSI 데이터 부족");
			return false;
		}

		DualIndicatorPoint current = stochList.get(stochList.size() - 1);
		DualIndicatorPoint previous = stochList.get(stochList.size() - 2);

		if (current.getK() == null || previous.getK() == null) {
			log.log("❌ StochRSI null 값 존재 → 평가 불가");
			return false;
		}

		// 평균 반등 %K 계산
		double sum = 0;
		int count = 0;
		int start = Math.max(0, stochList.size() - STOCH_LOOKBACK - 1);
		for (int i = start; i < stochList.size() - 1; i++) {
			DualIndicatorPoint a = stochList.get(i);
			DualIndicatorPoint b = stochList.get(i + 1);
			if (a.getK() != null && b.getK() != null && a.getK() < 20 && b.getK() > a.getK()) {
				sum += a.getK();
				count++;
			}
		}

		double avgBounceK = (count > 0) ? (sum / count) : 10.0;
		double threshold = avgBounceK * STOCH_BOUNCE_THRESHOLD_MULTIPLIER;
		boolean stochOk = current.getK() <= threshold && current.getK() > previous.getK();

		log
			.log("📈 StochRSI K: {}, avgBounce: {}, threshold: {}, rising: {}", current.getK(), avgBounceK, threshold,
					current.getK() > previous.getK());

		if (!stochOk) {
			log.log("❌ StochRSI 조건 불충족");
			return false;
		}

		log.log("✅ 스탑헌팅 + StochRSI 조건 만족: 진입 허용");
		return true;
	}
}
