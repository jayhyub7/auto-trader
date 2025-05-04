package com.auto.trader.exchange.impl;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

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
            String url = BASE_URL + ACCOUNT_PATH + "?" + queryString;
            HttpHeaders headers = buildHeaders(ACCOUNT_PATH, key, queryString);

            ResponseEntity<Map<String, Object>> responseEntity = getWithHeaders(url, headers);
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
            String url = BASE_URL + ACCOUNT_PATH + "?" + queryString;
            HttpHeaders headers = buildHeaders(ACCOUNT_PATH, key, queryString);

            getWithHeaders(url, headers);
            return true;
        } catch (Exception e) {
            log.error("❌ Bitget 인증 실패", e);
            return false;
        }
    }

    public HttpHeaders buildHeaders(String requestPath, ApiKey apiKey, String queryString) {
        try {
            String serverTimeUrl = BASE_URL + "/api/v2/public/time";
            Map<String, Object> timeResponse = getWithHeaders(serverTimeUrl, new HttpHeaders()).getBody();
            if (timeResponse == null || !timeResponse.containsKey("data")) {
                throw new IllegalStateException("Bitget 서버 시간 응답이 잘못되었습니다.");
            }

            String timestamp = String.valueOf(((Map<String, Object>) timeResponse.get("data")).get("serverTime"));
            String method = "GET";
            String body = "";
            String preHash = timestamp + method + requestPath + "?" + queryString;

            String sign = hmacSha256WithBase64Encoding(preHash, apiKey.getSecretKey().trim());

            log.info("🧾 preHash: {}", preHash);
            log.info("🧾 ACCESS-SIGN: {}", sign);
            log.info("🧾 queryString (raw): '{}'", queryString);
            log.info("🧾 requestPath: '{}'", requestPath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("ACCESS-KEY", apiKey.getApiKey());
            headers.set("ACCESS-TIMESTAMP", timestamp);
            headers.set("ACCESS-SIGN", sign);
            headers.set("ACCESS-PASSPHRASE", apiKey.getPassphrase());
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            return headers;
        } catch (Exception e) {
            throw new RuntimeException("❌ Bitget Header 생성 실패", e);
        }
    }

    @Override
    public HttpHeaders buildHeaders(ApiKey apiKey) {
        return null;
    }
}
