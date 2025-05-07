package com.auto.trader.exchange.impl;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.dto.SignedRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BinanceServiceImpl extends AbstractExchangeService implements ExchangeService {

    private static final String BASE_URL = "https://api.binance.com";
    private static final String ACCOUNT_PATH = "/api/v3/account";

    @Override
    public boolean supports(Exchange exchange) {
        return exchange == Exchange.BINANCE;
    }

    @Override
    public List<BalanceDto> fetchBalances(ApiKey key) {
        try {
            SignedRequest signed = buildSignedRequest(key, null, null);
            String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();
            Map<String, Object> response = getWithHeaders(url, signed.getHeaders()).getBody();
            List<Map<String, Object>> rawBalances = (List<Map<String, Object>>) response.get("balances");
            return parseBalances(rawBalances);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean validate(ApiKey key) {
        try {
            SignedRequest signed = buildSignedRequest(key, null, null);
            String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();
            getWithHeaders(url, signed.getHeaders());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected List<BalanceDto> parseBalances(List<Map<String, Object>> rawBalances) {
        return rawBalances.stream()
            .map(b -> {
                String asset = (String) b.get("asset");
                double available = Double.parseDouble(b.get("free").toString());
                double locked = Double.parseDouble(b.get("locked").toString());
                return toBalanceDto(asset, available, locked);
            })
            .filter(dto -> dto.getTotal() > 0)
            .toList();
    }

    @Override
    public SignedRequest buildSignedRequest(ApiKey apiKey, String unusedPath, String unusedQuery) {
        long timestamp = fetchBinanceServerTime();
        String recvWindow = "10000";
        String queryString = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;
        String signature = hmacSha256(queryString, apiKey.getSecretKey());
        String fullQuery = queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey.getApiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return new SignedRequest(headers, fullQuery);
    }

    private long fetchBinanceServerTime() {
        String url = BASE_URL + "/api/v3/time";
        Map<String, Object> response = getWithHeaders(url, new HttpHeaders()).getBody();
        if (response == null || !response.containsKey("serverTime")) {
            throw new IllegalStateException("Binance 서버 시간 응답 오류");
        }
        return Long.parseLong(response.get("serverTime").toString());
    }
}
