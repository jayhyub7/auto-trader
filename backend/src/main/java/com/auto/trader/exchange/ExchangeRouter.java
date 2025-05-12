package com.auto.trader.exchange;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.impl.BinanceServiceImpl;
import com.auto.trader.exchange.impl.BitgetServiceImpl;
import com.auto.trader.exchange.impl.BybitServiceImpl;
import com.auto.trader.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRouter {

  private final BinanceServiceImpl binanceService;
  private final BybitServiceImpl bybitService;
  private final BitgetServiceImpl bitgetService;
  private final ApiKeyService apiKeyService;

  private static final Map<Exchange, ExchangeService> serviceMap = new EnumMap<>(Exchange.class);

  @jakarta.annotation.PostConstruct
  public void init() {
    serviceMap.put(Exchange.BINANCE, binanceService);
    serviceMap.put(Exchange.BYBIT, bybitService);
    serviceMap.put(Exchange.BITGET, bitgetService);
  }

  public ExchangeService getService(Exchange exchange) {
    ExchangeService service = serviceMap.get(exchange);
    if (service == null) {
      throw new UnsupportedOperationException("지원하지 않는 거래소: " + exchange);
    }
    return service;
  }



}
