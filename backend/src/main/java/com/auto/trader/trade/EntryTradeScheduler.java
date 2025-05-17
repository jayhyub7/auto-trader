// 📄 EntryTradeScheduler.java

package com.auto.trader.trade;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.exchange.ExchangeRouter;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.Timeframe;
import com.auto.trader.position.repository.PositionOpenRepository;
import com.auto.trader.position.service.PositionOpenService;
import com.auto.trader.position.service.PositionService;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.service.ApiKeyService;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.repository.TradeConditionRepository;
import com.auto.trader.trade.repository.TradeLogRepository;
import com.auto.trader.trade.service.ExecutedOrderService;
import com.auto.trader.trade.service.TradeLogService;
import com.auto.trader.trade.util.PositionLogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EntryTradeScheduler {

	private final PositionService positionService;
	private final PositionOpenService positionOpenService;
	private final ObjectMapper objectMapper;
	private final PositionOpenRepository positionOpenRepository;
	private final TradeLogService tradeLogService;
	private final TradeLogRepository tradeLogRepository;
	private final TradeConditionRepository tradeConditionRepository;
	private final ExecutedOrderService executedOrderService;
	private final ExchangeRouter exchangeRouter;
	private final ApiKeyService apiKeyService;
	private final SchedulerLogManager entryLogManager;

	public EntryTradeScheduler(PositionService positionService, PositionOpenService positionOpenService,
			ObjectMapper objectMapper, PositionOpenRepository positionOpenRepository, TradeLogService tradeLogService,
			TradeLogRepository tradeLogRepository, TradeConditionRepository tradeConditionRepository,
			ExecutedOrderService executedOrderService, ExchangeRouter exchangeRouter, ApiKeyService apiKeyService,
			@Qualifier("entryLogManager") SchedulerLogManager entryLogManager) {
		this.positionService = positionService;
		this.positionOpenService = positionOpenService;
		this.objectMapper = objectMapper;
		this.positionOpenRepository = positionOpenRepository;
		this.tradeLogService = tradeLogService;
		this.tradeLogRepository = tradeLogRepository;
		this.tradeConditionRepository = tradeConditionRepository;
		this.executedOrderService = executedOrderService;
		this.exchangeRouter = exchangeRouter;
		this.apiKeyService = apiKeyService;
		this.entryLogManager = entryLogManager;
	}

	@Scheduled(fixedDelay = 1000)
	@Transactional
	public void checkEntryPosition() {

		if (!entryLogManager.isEnabled())
			return;

		List<Position> activePositions = positionOpenService.findEntryPosition();
		for (Position p : activePositions) {
			p.getConditions().size();
		}

		for (Position position : activePositions) {
			entryLogManager.log("━━━━━━━━━━━━━━━ [🔍 포지션 검사 시작] ━━━━━━━━━━━━━━━");
			PositionLogUtil.log(position);

			PositionOpen positionOpen = position.getPositionOpenList().get(0);
			if (positionOpen.isExecuted()) {
				entryLogManager.log("⏸️ 중복 방지: 이미 실행된 포지션 - {}", position.getTitle());
				continue;
			}

			boolean isPass = true;

			try {
				for (var cond : position.getConditions()) {
					Timeframe timeframe = cond.getTimeframe();
					String key = "BTCUSDT_" + timeframe.getLabel();
					IndicatorCache cache = IndicatorMemoryStore.get(key);

					entryLogManager
						.log("🧩 [조건 평가 시작] - 타입: {}, 연산자: {}, 타임프레임: {}", cond.getType(), cond.getOperator(),
								timeframe);

					if (cache == null) {
						entryLogManager.log("⚠️ 지표 캐시 없음: {}", key);
						isPass = false;
						break;
					}

					switch (cond.getType()) {
					case RSI -> {
						var value = cond.getValue();
						var rsiList = cache.getRsi();

						if (!rsiList.isEmpty()) {
							var latest = rsiList.get(rsiList.size() - 1);
							double currentRsi = latest.getValue();
							entryLogManager
								.log("📈 [RSI 검사] 현재: {}, 기준: {}, 연산자: {}", currentRsi, value, cond.getOperator());

							if (cond.getOperator() == Operator.이상) {
								if (currentRsi < value) {
									entryLogManager.log("❌ RSI 조건 실패: {} < {}", currentRsi, value);
									isPass = false;
								} else {
									entryLogManager.log("✅ RSI 조건 통과");
								}
							} else if (cond.getOperator() == Operator.이하) {
								if (currentRsi > value) {
									entryLogManager.log("❌ RSI 조건 실패: {} > {}", currentRsi, value);
									isPass = false;
								} else {
									entryLogManager.log("✅ RSI 조건 통과");
								}
							}
						} else {
							entryLogManager.log("⚠️ RSI 리스트 비어 있음");
							isPass = false;
						}
					}

					case STOCH_RSI -> {
						var value = cond.getValue();
						var kTarget = cond.getK();
						var dTarget = cond.getD();
						var stochList = cache.getStochRsi();

						if (!stochList.isEmpty()) {
							var latest = stochList.get(stochList.size() - 1);
							double currentK = latest.getK();
							double currentD = latest.getD();
							entryLogManager
								.log("📉 [StochRSI 검사] K: {}, D: {}, 기준: {}, 연산자: {}", currentK, currentD, value,
										cond.getOperator());

							if (cond.getOperator() == Operator.이상 && currentK < value) {
								entryLogManager.log("❌ K값 조건 실패: {} < {}", currentK, value);
								isPass = false;
							} else if (cond.getOperator() == Operator.이하 && currentK > value) {
								entryLogManager.log("❌ K값 조건 실패: {} > {}", currentK, value);
								isPass = false;
							} else {
								entryLogManager.log("✅ K값 조건 통과");
							}

							if (kTarget != null && dTarget != null) {
								if (currentK > currentD && currentK - currentD >= 0.5) {
									entryLogManager.log("✅ 교차 조건 통과 (%K > %D)");
								} else {
									entryLogManager
										.log("❌ 교차 조건 실패 (%K={}, %D={}, 차이={})", currentK, currentD,
												currentK - currentD);
									isPass = false;
								}
							}
						} else {
							entryLogManager.log("⚠️ StochRSI 리스트 비어 있음");
							isPass = false;
						}
					}

					case VWBB -> {
						var basis = cache.getVwbb().getBasis();
						var upper = cache.getVwbb().getUpper();
						var lower = cache.getVwbb().getLower();
						double currentPrice = cache.getCurrentPrice();

						if (!basis.isEmpty()) {
							int size = basis.size();
							double upperBand = upper.get(size - 1).getValue();
							double lowerBand = lower.get(size - 1).getValue();
							double basisVal = basis.get(size - 1).getValue();
							long lastCandleTime = cache.getCandles().get(cache.getCandles().size() - 1).getTime();

							entryLogManager
								.log("📊 [VWBB 검사] 현재가: {}, 상단: {}, 기준선: {}, 하단: {}, 캔들 수: {}, 마지막 캔들 UTC: {}",
										currentPrice, upperBand, basisVal, lowerBand, cache.getCandles().size(),
										lastCandleTime);

							if (cond.getOperator() == Operator.상단_돌파) {
								if (currentPrice > upperBand) {
									entryLogManager.log("✅ 상단 돌파 조건 통과 ({} > {})", currentPrice, upperBand);
								} else {
									entryLogManager.log("❌ 상단 돌파 조건 실패 ({} <= {})", currentPrice, upperBand);
									isPass = false;
								}
							}

							if (cond.getOperator() == Operator.하단_돌파) {
								if (currentPrice < lowerBand) {
									entryLogManager.log("✅ 하단 돌파 조건 통과 ({} < {})", currentPrice, lowerBand);
								} else {
									entryLogManager.log("❌ 하단 돌파 조건 실패 ({} >= {})", currentPrice, lowerBand);
									isPass = false;
								}
							}
						} else {
							entryLogManager.log("⚠️ VWBB 기준선 없음");
							isPass = false;
						}
					}
					}

					if (!isPass) {
						entryLogManager.log("❌ 조건 미충족 → 다음 포지션으로");
						break;
					}
				}

				if (isPass) {
					entryLogManager.log("🚀 진입 조건 만족 → 매매 실행 예정: {}", position.getTitle());
				}

			} catch (Exception e) {
				log.error("🚨 포지션 처리 중 오류: " + position.getId(), e);
			}
		}
	}

	private double calcStopLossPrice(double entryPrice, double percent, Direction direction) {
		return direction == Direction.LONG ? entryPrice * (1 - percent) : entryPrice * (1 + percent);
	}

	private double calcTakeProfitPrice(double entryPrice, double percent, Direction direction) {
		return direction == Direction.LONG ? entryPrice * (1 + percent) : entryPrice * (1 - percent);
	}
}