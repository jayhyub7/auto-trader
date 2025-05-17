// 파일: ExitTradeScheduler.java

package com.auto.trader.trade;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.balance.dto.cache.BalanceMemoryStore;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.exchange.ExchangeRouter;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.AmountType;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.PositionOpenStatus;
import com.auto.trader.position.repository.PositionOpenRepository;
import com.auto.trader.position.service.PositionOpenService;
import com.auto.trader.position.service.PositionService;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.service.ApiKeyService;
import com.auto.trader.trade.entity.ExecutedOrder;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.repository.ExecutedOrderRepository;
import com.auto.trader.trade.service.ExecutedOrderService;
import com.auto.trader.trade.service.TradeLogService;
import com.auto.trader.trade.util.PositionLogUtil;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExitTradeScheduler {

	private final PositionService positionService;
	private final IndicatorMemoryStore indicatorStore;
	private final PositionOpenService positionOpenService;
	private final ExchangeRouter exchangeRouter;
	private final ExecutedOrderService executedOrderService;
	private final ExecutedOrderRepository executedOrderRepository;
	private final ApiKeyService apiKeyService;
	private final TradeLogService tradeLogService;
	private final PositionOpenRepository positionOpenRepository;
	private final SchedulerLogManager exitLogManager;

	public ExitTradeScheduler(PositionService positionService, IndicatorMemoryStore indicatorStore,
			PositionOpenService positionOpenService, ExchangeRouter exchangeRouter,
			ExecutedOrderService executedOrderService, ExecutedOrderRepository executedOrderRepository,
			ApiKeyService apiKeyService, TradeLogService tradeLogService, PositionOpenRepository positionOpenRepository,
			@Qualifier("exitLogManager") SchedulerLogManager exitLogManager) {
		this.positionService = positionService;
		this.indicatorStore = indicatorStore;
		this.positionOpenService = positionOpenService;
		this.exchangeRouter = exchangeRouter;
		this.executedOrderService = executedOrderService;
		this.executedOrderRepository = executedOrderRepository;
		this.apiKeyService = apiKeyService;
		this.tradeLogService = tradeLogService;
		this.positionOpenRepository = positionOpenRepository;
		this.exitLogManager = exitLogManager;
	}

	@Scheduled(fixedDelay = 3000)
	@Transactional
	public void checkExitPosition() {
		if (!exitLogManager.isEnabled())
			return;

		List<Position> runningPositions = positionOpenService.findRunningPositions();
		for (Position p : runningPositions) {
			p.getConditions().size();
		}

		for (Position position : runningPositions) {
			exitLogManager.log("🔍 종료 조건 확인 중: {}", position.getTitle());

			PositionOpen positionOpen = position.getPositionOpenList().get(0);
			ExecutedOrder executed = executedOrderRepository
				.findByOrderId(positionOpen.getCurrentOrderId())
				.orElse(null);

			if (executed == null) {
				exitLogManager.log("❌ 체결된 주문을 찾을 수 없습니다. - {}", positionOpen.getCurrentOrderId());
				continue;
			}

			double stopLossPrice;
			if (positionOpen.getAmountType() == AmountType.PERCENT) {
				double entryPrice = executed.getExecutedPrice();
				stopLossPrice = calcStopLossPrice(entryPrice, positionOpen.getStopLoss(), position.getDirection());
			} else {
				stopLossPrice = positionOpen.getStopLoss();
			}

			double currentPrice = indicatorStore.get("BTCUSDT_1m").getCurrentPrice();

			boolean isStopLossHit = (position.getDirection() == Direction.LONG && currentPrice <= stopLossPrice)
					|| (position.getDirection() == Direction.SHORT && currentPrice >= stopLossPrice);

			if (isStopLossHit) {
				exitLogManager.log("🛑 StopLoss 조건 만족 → 강제 종료 트리거: 현재가={}, 손절가={}", currentPrice, stopLossPrice);
			}

			boolean isPass = true;
			for (var cond : position.getConditions()) {
				if (isStopLossHit)
					break;

				String key = "BTCUSDT_" + cond.getTimeframe().getLabel();
				IndicatorCache cache = indicatorStore.get(key);

				if (cache == null) {
					exitLogManager.log("⚠️ 지표 캐시 없음: {}", key);
					isPass = false;
					break;
				}

				Operator operator = cond.getOperator();

				switch (cond.getType()) {
				case RSI -> {
					var value = cond.getValue();
					var rsiList = cache.getRsi();
					if (!rsiList.isEmpty()) {
						double currentRsi = rsiList.get(rsiList.size() - 1).getValue();
						exitLogManager.log("🧪 RSI 검사 | 현재: {}, 기준: {}, 연산자: {}", currentRsi, value, operator);
						if (operator == Operator.이상 && currentRsi < value
								|| operator == Operator.이하 && currentRsi > value) {
							exitLogManager.log("❌ RSI 실패");
							isPass = false;
						} else {
							exitLogManager.log("✅ RSI 통과");
						}
					} else {
						exitLogManager.log("⚠️ RSI 리스트 비어 있음");
						isPass = false;
					}
				}

				case STOCH_RSI -> {
					var value = cond.getValue();
					var stochList = cache.getStochRsi();
					if (!stochList.isEmpty()) {
						var latest = stochList.get(stochList.size() - 1);
						double k = latest.getK(), d = latest.getD();
						if (operator == Operator.이상 && k < value || operator == Operator.이하 && k > value) {
							exitLogManager.log("❌ StochRSI 실패");
							isPass = false;
						} else {
							exitLogManager.log("✅ StochRSI 통과");
						}
						if (cond.getK() != null && cond.getD() != null) {
							if (!(k > d && k - d >= 0.5)) {
								exitLogManager.log("❌ 교차 조건 실패");
								isPass = false;
							}
						}
					} else {
						exitLogManager.log("⚠️ StochRSI 리스트 비어 있음");
						isPass = false;
					}
				}

				case VWBB -> {
					var vwbb = cache.getVwbb();
					if (!vwbb.getBasis().isEmpty()) {
						double upper = vwbb.getUpper().getLast().getValue();
						double lower = vwbb.getLower().getLast().getValue();

						exitLogManager
							.log("🧪 VWBB 검사 | 현재가: {}, 상단: {}, 하단: {}, 연산자: {}", currentPrice, upper, lower, operator);

						if (operator == Operator.상단_돌파) {
							if (currentPrice > upper) {
								exitLogManager.log("✅ 상단 돌파 조건 통과 ({} > {})", currentPrice, upper);
							} else {
								exitLogManager.log("❌ 상단 돌파 조건 실패 ({} <= {})", currentPrice, upper);
								isPass = false;
							}
						} else if (operator == Operator.하단_돌파) {
							if (currentPrice < lower) {
								exitLogManager.log("✅ 하단 돌파 조건 통과 ({} < {})", currentPrice, lower);
							} else {
								exitLogManager.log("❌ 하단 돌파 조건 실패 ({} >= {})", currentPrice, lower);
								isPass = false;
							}
						} else {
							exitLogManager.log("⚠️ 지원하지 않는 VWBB 연산자: {}", operator);
							isPass = false;
						}
					} else {
						exitLogManager.log("⚠️ VWBB 기준선 없음");
						isPass = false;
					}
				}
				}

				if (!isPass)
					break;
			}

			if (isPass || isStopLossHit) {
				exitLogManager.log("✅ 종료 조건 만족 → 매매 실행 예정: {}", position.getTitle());

				ApiKey apiKey = apiKeyService.getValidatedKey(position.getUser(), position.getExchange());

				if (!positionOpen.isValidStopLoss()) {
					throw new IllegalStateException("❌ stopLoss 누락 또는 값 이상 (Position ID: " + position.getId() + ")");
				}

				double observedPrice = currentPrice;

				double quantity;
				if (positionOpen.getAmountType() == AmountType.PERCENT) {
					List<BalanceDto> balances = BalanceMemoryStore.get(position.getUser().getId());
					double available = balances
						.stream()
						.filter(b -> b.getAsset().equalsIgnoreCase("USDT") && b.getExchange() == position.getExchange())
						.mapToDouble(BalanceDto::getAvailable)
						.findFirst()
						.orElse(0.0);
					quantity = available * (positionOpen.getAmount() / 100.0);
					exitLogManager.log("📊 종료 주문 수량 계산: available={} → quantity={}", available, quantity);
				} else {
					quantity = positionOpen.getAmount();
				}

				ExchangeService exchangeService = exchangeRouter.getService(position.getExchange());
				OrderResult result = null;
				if (positionOpen.getStatus().equals(PositionOpenStatus.RUNNING)) {
					result = exchangeService
						.placeMarketOrder(apiKey, "BTCUSDT", quantity, position.getDirection(), null, null);
				} else {
					result = exchangeService.createSimulatedOrder("BTCUSDT", quantity, observedPrice);
				}

				if (!result.isSuccess()) {
					throw new IllegalStateException("❌ 종료 시장가 주문 실패: " + result.getRawResponse());
				}

				executedOrderService
					.saveExecutedOrderWithIndicators(result, positionOpen, position.getExchange().name(), "BTCUSDT",
							observedPrice);

				PositionLogUtil.log(position);
				tradeLogService.saveTradeLogWithConditions(result, position, positionOpen);

				positionOpen.setExecuted(false);
				positionOpenRepository.save(positionOpen);
			}
		}
	}

	private double calcStopLossPrice(double entryPrice, double percent, Direction direction) {
		return direction == Direction.LONG ? entryPrice * (1 - percent) : entryPrice * (1 + percent);
	}
}