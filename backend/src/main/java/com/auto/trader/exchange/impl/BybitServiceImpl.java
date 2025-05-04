
package com.auto.trader.exchange.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;

@Service
public class BybitServiceImpl extends AbstractExchangeService implements ExchangeService {

    private static final String BASE_URL = "https://api.bybit.com";
    private static final String ACCOUNT_PATH = "/v5/account/wallet-balance";

    @Override
    public boolean supports(Exchange exchange) {
        return exchange == Exchange.BYBIT;
    }

    @Override
    public List<BalanceDto> fetchBalances(ApiKey key) {
        try {
            String url = BASE_URL + ACCOUNT_PATH + "?accountType=UNIFIED";
            Map<String, Object> response = getWithHeaders(url, buildHeaders(key, "accountType=UNIFIED")).getBody();
            List<Map<String, Object>> rawBalances = (List<Map<String, Object>>) ((Map<String, Object>) response.get("result")).get("list");
            return parseBalances(rawBalances);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean validate(ApiKey key) {
        try {
            String url = BASE_URL + ACCOUNT_PATH + "?accountType=UNIFIED";
            Map<String, Object> response = getWithHeaders(url, buildHeaders(key, "accountType=UNIFIED")).getBody();           
            return true;
        } catch (Exception e) {
            System.out.println("Bybit validate failed: " + e.getMessage());
            return false;
        }
    }

    protected List<BalanceDto> parseBalances(List<Map<String, Object>> rawBalances) {
        List<BalanceDto> result = new ArrayList<>();

        for (Map<String, Object> balanceMap : rawBalances) {
            Object coinObj = balanceMap.get("coin");
            if (!(coinObj instanceof List<?>)) continue;

            List<?> coins = (List<?>) coinObj;
            for (Object c : coins) {
                if (!(c instanceof Map)) continue;

                Map<String, Object> coinMap = (Map<String, Object>) c;
                String asset = (String) coinMap.get("coin");
                double available = parseDouble(coinMap.get("walletBalance"));
                double locked = 0.0;
                double usdValue = parseDouble(coinMap.get("usdValue"));

                BalanceDto dto = new BalanceDto(asset, available, locked, available, usdValue);
                if (dto.getTotal() > 0) {
                    result.add(dto);
                }
            }
        }

        return result;
    }
    
    private double parseDouble(Object value) {
        try {
            return value != null ? Double.parseDouble(value.toString()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public HttpHeaders buildHeaders(ApiKey apiKey, String queryString) {
        long timestamp = System.currentTimeMillis();
        String recvWindow = "5000";
        String payload = timestamp + apiKey.getApiKey() + recvWindow + queryString;
        String signature = hmacSha256(payload, apiKey.getSecretKey());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-BAPI-API-KEY", apiKey.getApiKey());
        headers.set("X-BAPI-SIGN", signature);
        headers.set("X-BAPI-TIMESTAMP", String.valueOf(timestamp));
        headers.set("X-BAPI-RECV-WINDOW", recvWindow);
        headers.set("X-BAPI-SIGN-TYPE", "2");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

	@Override
	public HttpHeaders buildHeaders(ApiKey apiKey) {
		// TODO Auto-generated method stub
		return null;
	}
}
