package com.auto.trader.trade.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.Side;
import com.auto.trader.position.enums.Timeframe;
import com.auto.trader.trade.entity.ExecutedIndicator;
import com.auto.trader.trade.entity.ExecutedOrder;
import com.auto.trader.trade.enums.OrderType;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.repository.ExecutedIndicatorRepository;
import com.auto.trader.trade.repository.ExecutedOrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutedOrderService {

	private final ExecutedOrderRepository executedOrderRepository;
	private final ExecutedIndicatorRepository executedIndicatorRepository;

	public ExecutedOrder saveExecutedOrderWithIndicators(OrderResult result, Side side, Direction direction,
			PositionOpen positionOpen, String exchange, String symbol, double observedPrice, String executionLog) {

		Double profitPercent = null;
		if (side == Side.EXIT) {
			double executed = result.getPrice();
			double observed = observedPrice; // 진입 당시 가격
			profitPercent = direction == Direction.LONG ? (executed - observed) / observed * 100
					: (observed - executed) / observed * 100;
		}
		// 1. 체결 정보 저장
		ExecutedOrder order = ExecutedOrder
			.builder()
			.exchange(exchange)
			.symbol(symbol)
			.side(side)
			.direction(direction)
			.quantity(positionOpen.getAmount())
			.observedPrice(observedPrice)
			.executedPrice(result.getPrice())
			.orderId(result.getOrderId())
			.success(result.isSuccess())
			.rawResponse(result.getRawResponse())
			.executedAt(LocalDateTime.now())
			.simulation(positionOpen.getPosition().isSimulation())
			.positionOpen(positionOpen)
			.orderType(OrderType.MARKET)
			.leverage(positionOpen.getLeverage())
			.profitPercent(profitPercent)
			.build();

		order.setFeeAmount(result.getFeeAmount());
		order.setFeeCurrency(result.getFeeCurrency());
		order.setFeeRate(result.getFeeRate());

		ExecutedOrder saved = executedOrderRepository.save(order);

		// 2. 모든 Timeframe에 대한 지표값 저장
		for (Timeframe tf : Timeframe.values()) {
			String key = symbol + "_" + tf.getLabel();
			IndicatorCache cache = IndicatorMemoryStore.get(key);
			if (cache == null)
				continue;

			var rsi = lastOrNull(cache.getRsi());
			var stoch = lastOrNull(cache.getStochRsi());
			var basis = lastOrNull(cache.getVwbb().getBasis());
			var upper = lastOrNull(cache.getVwbb().getUpper());
			var lower = lastOrNull(cache.getVwbb().getLower());

			ExecutedIndicator indicator = ExecutedIndicator
				.builder()
				.executedOrder(saved)
				.timeframe(tf)
				.rsi(rsi != null ? rsi.getValue() : null)
				.stochK(stoch != null ? stoch.getK() : null)
				.stochD(stoch != null ? stoch.getD() : null)
				.vwbbBasis(basis != null ? basis.getValue() : null)
				.vwbbUpper(upper != null ? upper.getValue() : null)
				.vwbbLower(lower != null ? lower.getValue() : null)
				.currentPrice(cache.getCurrentPrice())
				.build();

			executedIndicatorRepository.save(indicator);
		}

		log
			.info("✅ 체결 로그 + 지표 스냅샷 저장 완료: orderId={}, symbol={}, qty={}", saved.getOrderId(), saved.getSymbol(),
					saved.getQuantity());

		return saved;
	}

	private <T> T lastOrNull(java.util.List<T> list) {
		return (list == null || list.isEmpty()) ? null : list.get(list.size() - 1);
	}
}
