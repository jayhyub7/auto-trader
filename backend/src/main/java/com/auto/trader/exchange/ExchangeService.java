package com.auto.trader.exchange;

import java.util.List;

import org.springframework.http.HttpMethod;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.dto.OrderFeeResult;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.exchange.dto.SignedRequest;
import com.auto.trader.position.enums.Direction;

public interface ExchangeService {
	boolean supports(Exchange exchange);

	boolean validate(ApiKey key);

	List<BalanceDto> fetchBalances(ApiKey apiKey);

	// ✅ HttpMethod 인자 추가
	SignedRequest buildSignedRequest(ApiKey apiKey, String path, String payload, HttpMethod method);

	OrderResult placeMarketOrder(ApiKey key, String symbol, double quantity, Direction direction, Double stopLossPrice,
			Double takeProfitPrice);

	boolean placeStopLossOrder(ApiKey key, String symbol, double quantity, double stopLossPrice, Direction direction);

	boolean placeTakeProfitOrder(ApiKey key, String symbol, double quantity, double takeProfitPrice,
			Direction direction);

	default OrderResult createSimulatedOrder(String symbol, double quantity, double price) {
		OrderResult result = new OrderResult();
		result.setSuccess(true);
		result.setOrderId("SIMULATED_" + System.currentTimeMillis());
		result.setSymbol(symbol);
		result.setExecutedQty(quantity);
		result.setPrice(price);
		result.setRawResponse("SIMULATED ORDER");
		result.setTpSlSuccess(false);
		result.setExecutionTimeSeconds(0.0);
		return result;
	}

	void setLeverage(ApiKey apiKey, String symbol, int leverage);

	OrderFeeResult fetchOrderFee(ApiKey key, String symbol, String orderId);

}
