package com.auto.trader.rate;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RateService {

	private static final String API_URL = "https://api.exchangerate.host/latest?base=USD&symbols=KRW";

    private final RestTemplate restTemplate = new RestTemplate();
    private double usdToKrw = 1350.0; // 초기 fallback

    public void updateRates() {
        try {
            ExchangeRateResponse response = restTemplate.getForObject(API_URL, ExchangeRateResponse.class);
            log.info("🔍 API 응답: {}", response);
            if (response != null && response.getRates() != null && response.getRates().getKrw() > 0) {
                usdToKrw = response.getRates().getKrw();
                log.info("✅ 환율 업데이트: 1 USD = {} KRW", usdToKrw);
            } else {
                log.warn("⚠️ 환율 응답이 유효하지 않음");
            }
        } catch (Exception e) {
            log.error("❌ 환율 조회 실패", e);
        }
    }

    public double getUsdToKrw() {
        return usdToKrw;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExchangeRateResponse {
        private Rates rates;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Rates {
            @JsonProperty("KRW")
            private double krw;
        }
    }
}
