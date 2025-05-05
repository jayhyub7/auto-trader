package com.auto.trader.exchange.impl;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;
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
            SignedQuery signed = buildHeaders(key, null);
            String url = BASE_URL + ACCOUNT_PATH + "?" + signed.queryStringWithSig;
            Map<String, Object> response = getWithHeaders(url, signed.headers).getBody();
            List<Map<String, Object>> rawBalances = (List<Map<String, Object>>) response.get("balances");
            return parseBalances(rawBalances);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean validate(ApiKey key) {
        try {
            SignedQuery signed = buildHeaders(key, null);
            String url = BASE_URL + ACCOUNT_PATH + "?" + signed.queryStringWithSig;
            getWithHeaders(url, signed.headers);
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

    // 🧾 Binance 서버 시간 기반 인증 헤더 + 서명 포함된 queryString 반환
    public SignedQuery buildHeaders(ApiKey apiKey, String unused) {
        try {
            long timestamp = fetchBinanceServerTime();
            String recvWindow = "10000";
            String queryString = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;
            String signature = hmacSha256(queryString, apiKey.getSecretKey());
            String queryStringWithSig = queryString + "&signature=" + signature;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey.getApiKey());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            return new SignedQuery(headers, queryStringWithSig);
        } catch (Exception e) {
            throw new RuntimeException("❌ Binance Header 생성 실패", e);
        }
    }

    // Binance 서버 시간 가져오기
    private long fetchBinanceServerTime() {
        String url = BASE_URL + "/api/v3/time";
        Map<String, Object> response = getWithHeaders(url, new HttpHeaders()).getBody();
        if (response == null || !response.containsKey("serverTime")) {
            throw new IllegalStateException("Binance 서버 시간 응답 오류");
        }
        return Long.parseLong(response.get("serverTime").toString());
    }

    // queryString + headers 묶음 객체
    public static class SignedQuery {
        public final HttpHeaders headers;
        public final String queryStringWithSig;

        public SignedQuery(HttpHeaders headers, String queryStringWithSig) {
            this.headers = headers;
            this.queryStringWithSig = queryStringWithSig;
        }
    }

	@Override
	public HttpHeaders buildHeaders(ApiKey apiKey) {
		// TODO Auto-generated method stub
		return null;
	}
}
