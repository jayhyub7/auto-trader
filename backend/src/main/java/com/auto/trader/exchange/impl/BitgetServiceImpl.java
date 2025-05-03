package com.auto.trader.exchange.impl;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;

@Service
public class BitgetServiceImpl extends AbstractExchangeService implements ExchangeService {

    private static final String BASE_URL = "https://api.bitget.com";
    private static final String ACCOUNT_PATH = "/api/v2/account/assets";

    @Override
    public boolean supports(Exchange exchange) {
        return exchange == Exchange.BITGET;
    }

    @Override
    public List<BalanceDto> fetchBalances(ApiKey key) {
        try {
            String url = BASE_URL + ACCOUNT_PATH;
            Map<String, Object> response = getWithHeaders(url, buildHeaders(key)).getBody();
            List<Map<String, Object>> rawBalances = (List<Map<String, Object>>) response.get("data");
            return parseBalances(rawBalances);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean validate(ApiKey key) {
        try {
            String url = BASE_URL + ACCOUNT_PATH;
            getWithHeaders(url, buildHeaders(key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    protected List<BalanceDto> parseBalances(List<Map<String, Object>> rawBalances) {
        return rawBalances.stream()
            .map(b -> {
                String asset = (String) b.get("coin");
                double available = Double.parseDouble((String) b.get("available"));
                double locked = Double.parseDouble((String) b.getOrDefault("frozen", "0"));
                return toBalanceDto(asset, available, locked);
            })
            .filter(dto -> dto.getTotal() > 0)
            .toList();
    }

    @Override
    public HttpHeaders buildHeaders(ApiKey apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("ACCESS-KEY", apiKey.getApiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
