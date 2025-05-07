
package com.auto.trader.exchange;

import java.util.List;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.dto.SignedRequest;

public interface ExchangeService {
    boolean supports(Exchange exchange);
    boolean validate(ApiKey key);
    List<BalanceDto> fetchBalances(ApiKey apiKey);
    SignedRequest buildSignedRequest(ApiKey apiKey, String path, String queryString);
}
