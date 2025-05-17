// 파일: com.auto.trader.rate.RateService.java

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

	private static final String BINANCE_BTCUSDT_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
	private static final String UPBIT_BTCKRW_URL = "https://api.upbit.com/v1/ticker?markets=KRW-BTC";

	private final RestTemplate restTemplate = new RestTemplate();
	private double usdToKrw = 1350.0; // fallback 기본 환율

	public void updateRates() {
		try {
			BinancePrice btcUsdt = restTemplate.getForObject(BINANCE_BTCUSDT_URL, BinancePrice.class);
			UpbitPrice[] btcKrwArray = restTemplate.getForObject(UPBIT_BTCKRW_URL, UpbitPrice[].class);

			if (btcUsdt != null && btcUsdt.getPrice() > 0 && btcKrwArray != null && btcKrwArray.length > 0) {
				double btcUsdtPrice = btcUsdt.getPrice();
				double btcKrwPrice = btcKrwArray[0].getTradePrice();

				usdToKrw = btcKrwPrice / btcUsdtPrice;
				log.info("✅ 환율 업데이트: 1 USD ≒ {} KRW (BTC 기준 계산)", usdToKrw);
			} else {
				log.warn("⚠️ 가격 정보 부족 - 환율 업데이트 실패");
			}
		} catch (Exception e) {
			log.error("❌ 환율 계산 실패 (Binance/Upbit 연동)", e);
		}
	}

	public double getUsdToKrw() {
		return usdToKrw;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BinancePrice {
		private double price;

		@JsonProperty("price")
		public void setPrice(String price) {
			this.price = Double.parseDouble(price);
		}
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UpbitPrice {
		@JsonProperty("trade_price")
		private double tradePrice;
	}
}