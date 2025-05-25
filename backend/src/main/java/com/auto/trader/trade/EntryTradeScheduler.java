// 📄 EntryTradeScheduler.java

package com.auto.trader.trade;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.exchange.ExchangeRouter;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.AmountType;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.evaluator.entry.EntryConditionEvaluator;
import com.auto.trader.position.evaluator.entry.EntryEvaluatorRegistry;
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

		for (Position position : activePositions) {
			entryLogManager.log("━━━━━━━━━━━━━━━ [🔍 포지션 검사 시작] ━━━━━━━━━━━━━━━");
			PositionLogUtil.log(position);

			PositionOpen positionOpen = position.getPositionOpenList().get(0);
			if (positionOpen.isExecuted()) {
				entryLogManager.log("⏸️ 중복 방지: 이미 실행된 포지션 - {}", position.getTitle());
				continue;
			}

			if (positionOpen.getStopLoss() == 0) {
				entryLogManager.log("❌ StopLoss가 설정되지 않아 주문 실행 중단: {}", position.getTitle());
				continue;
			}

			boolean isPass = true;
			for (var cond : position.getConditions()) {
				String key = "BTCUSDT_" + cond.getTimeframe().getLabel();
				IndicatorCache cache = IndicatorMemoryStore.get(key);
				if (cache == null) {
					entryLogManager.log("⚠️ 지표 캐시 없음: {}", key);
					isPass = false;
					break;
				}

				EntryConditionEvaluator evaluator = EntryEvaluatorRegistry.get(cond.getType());
				if (evaluator == null) {
					entryLogManager.log("⚠️ 조건 평가기 없음: {}", cond.getType());
					isPass = false;
					break;
				}

				boolean passed = evaluator.evaluate(cond, cache, position.getDirection(), entryLogManager);
				if (!passed) {
					entryLogManager.log("❌ 조건 미충족 → 다음 포지션으로");
					isPass = false;
					break;
				}
			}

			if (isPass) {
				entryLogManager.log("🚀 진입 조건 만족 → 매매 실행 예정: {}", position.getTitle());

				var apiKey = apiKeyService.getValidatedKey(position.getUser(), position.getExchange());
				ExchangeService exchangeService = exchangeRouter.getService(position.getExchange());

				String symbol = "BTCUSDT";
				Direction direction = position.getDirection();
				double leverage = positionOpen.getLeverage();
				if (!position.isSimulation()) {
					exchangeService.setLeverage(apiKey, symbol, (int) leverage);
				}

				var cache = IndicatorMemoryStore.get("BTCUSDT_1m");
				if (cache == null || cache.getCandles().isEmpty()) {
					entryLogManager.log("⚠️ BTCUSDT_1m 캔들 없음 → 포지션 스킵");
					continue;
				}
				double observedPrice = cache.getCandles().get(cache.getCandles().size() - 1).getClose();

				double quantity;
				if (positionOpen.getAmountType() == AmountType.FIXED) {
					quantity = positionOpen.getAmount();
				} else {
					List<BalanceDto> balances = exchangeService.fetchBalances(apiKey);
					double availableUSDT = balances
						.stream()
						.filter(b -> b.getAsset().equals("USDT"))
						.findFirst()
						.map(BalanceDto::getAvailable)
						.orElse(0.0);
					double notional = availableUSDT * (positionOpen.getAmount() / 100.0);
					quantity = notional / observedPrice;
				}

				if (!position.isSimulation()) {
					if (quantity <= 0.0) {
						positionOpen.setErrorFlag(true);
						positionOpen.setErrorMessage("잔고 부족으로 진입 실패");
						positionOpenRepository.save(positionOpen);
						entryLogManager.log("❌ 잔고 부족으로 진입 생략: position={}", positionOpen.getId());
						continue;
					}
				}

				double slPercent = positionOpen.getStopLoss();
				Double tpPercent = positionOpen.getTakeProfit();
				Double slPrice = (slPercent > 0) ? calcStopLossPrice(observedPrice, slPercent / 100.0, direction)
						: null;
				Double tpPrice = (tpPercent != null && tpPercent > 0)
						? calcTakeProfitPrice(observedPrice, tpPercent / 100.0, direction)
						: null;

				long start = System.currentTimeMillis();

				OrderResult result;
				if (position.isSimulation()) {
					result = exchangeService.placeMarketOrder(apiKey, symbol, quantity, direction, slPrice, tpPrice);
				} else {
					result = exchangeService.createSimulatedOrder(symbol, quantity, observedPrice);
				}

				long end = System.currentTimeMillis();
				boolean slRegistered = false;
				boolean tpRegistered = false;

				if (slPercent > 0) {
					slPrice = calcStopLossPrice(result.getPrice(), slPercent / 100.0, direction);
					slRegistered = exchangeService.placeStopLossOrder(apiKey, symbol, quantity, slPrice, direction);
					entryLogManager.log(slRegistered ? "✅ SL 등록 성공" : "❌ SL 등록 실패");
				}

				if (tpPercent != null && tpPercent > 0) {
					tpPrice = calcTakeProfitPrice(result.getPrice(), tpPercent / 100.0, direction);
					tpRegistered = exchangeService.placeTakeProfitOrder(apiKey, symbol, quantity, tpPrice, direction);
					entryLogManager.log(tpRegistered ? "✅ TP 등록 성공" : "❌ TP 등록 실패");
				}

				result.setTpSlSuccess(slRegistered && tpRegistered);
				result.setExecutionTimeSeconds((end - start) / 1000.0);

				entryLogManager.log("✅ 시장가 주문 체결 완료. 주문ID: {}", result.getOrderId());
				entryLogManager.log("💰 체결 가격: {} (예상가: {})", result.getPrice(), observedPrice);
				entryLogManager.log("⏱️ 주문 실행 소요 시간: {}초", result.getExecutionTimeSeconds());

				executedOrderService
					.saveExecutedOrderWithIndicators(result, positionOpen, position.getExchange().name(), symbol,
							observedPrice);
				tradeLogService.saveTradeLogWithConditions(result, position, positionOpen);
				positionOpenRepository.save(positionOpen);
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
