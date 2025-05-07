package com.auto.trader.exchange.impl;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.dto.SignedRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BitgetServiceImpl extends AbstractExchangeService implements ExchangeService {

    private static final String BASE_URL = "https://api.bitget.com";
    private static final String ACCOUNT_PATH = "/api/v2/mix/account/account";

    @Override
    public boolean supports(Exchange exchange) {
        return exchange == Exchange.BITGET;
    }

    @Override
    public List<BalanceDto> fetchBalances(ApiKey key) {
        try {
            String queryString = "symbol=BTCUSDT&productType=USDT-FUTURES&marginCoin=USDT";
            SignedRequest signed = buildSignedRequest(key, ACCOUNT_PATH, queryString);
            String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();

            ResponseEntity<Map<String, Object>> responseEntity = getWithHeaders(url, signed.getHeaders());
            Map<String, Object> response = responseEntity.getBody();

            if (response == null || !response.containsKey("data")) return List.of();

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            String asset = (String) data.get("marginCoin");
            double available = Double.parseDouble((String) data.getOrDefault("available", "0"));
            double locked = Double.parseDouble((String) data.getOrDefault("locked", "0"));

            return List.of(toBalanceDto(asset, available, locked));
        } catch (Exception e) {
            log.error("❌ Bitget 잔고 조회 실패", e);
            return List.of();
        }
    }

    @Override
    public boolean validate(ApiKey key) {
        try {
            String queryString = "symbol=BTCUSDT&productType=USDT-FUTURES&marginCoin=USDT";
            SignedRequest signed = buildSignedRequest(key, ACCOUNT_PATH, queryString);
            String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();

            getWithHeaders(url, signed.getHeaders());
            return true;
        } catch (Exception e) {
            log.error("❌ Bitget 인증 실패", e);
            return false;
        }
    }

    @Override
    public SignedRequest buildSignedRequest(ApiKey apiKey, String path, String queryString) {
        try {
            String serverTimeUrl = BASE_URL + "/api/v2/public/time";
            Map<String, Object> timeResponse = getWithHeaders(serverTimeUrl, new HttpHeaders()).getBody();
            if (timeResponse == null || !timeResponse.containsKey("data")) {
                throw new IllegalStateException("Bitget 서버 시간 응답이 잘못되었습니다.");
            }

            String timestamp = String.valueOf(((Map<String, Object>) timeResponse.get("data")).get("serverTime"));
            String method = "GET";
            String body = "";
            String preHash = timestamp + method + path + "?" + queryString;
            String sign = hmacSha256WithBase64Encoding(preHash, apiKey.getSecretKey().trim());

            HttpHeaders headers = new HttpHeaders();
            headers.set("ACCESS-KEY", apiKey.getApiKey());
            headers.set("ACCESS-TIMESTAMP", timestamp);
            headers.set("ACCESS-SIGN", sign);
            headers.set("ACCESS-PASSPHRASE", apiKey.getPassphrase());
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            return new SignedRequest(headers, queryString);
        } catch (Exception e) {
            throw new RuntimeException("❌ Bitget Header 생성 실패", e);
        }
    }
}
