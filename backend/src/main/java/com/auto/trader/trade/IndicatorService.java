package com.auto.trader.trade;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.auto.trader.position.repository.PositionRepository;
import com.auto.trader.trade.dto.CandleDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorService {
	protected final RestTemplate restTemplate = new RestTemplate();
	public List<CandleDto> getCandles(String symbol, String interval, int limit) {
	    try {
	        String url = String.format("https://fapi.binance.com/fapi/v1/klines?symbol=%s&interval=%s&limit=%d",
	                                   symbol, interval, limit);

	        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, null, List.class);
	        List<List<Object>> raw = response.getBody();

	        List<CandleDto> result = new ArrayList<>();
	        for (List<Object> item : raw) {
	            CandleDto candle = new CandleDto();
	            candle.setTime(((Number) item.get(0)).longValue());
	            candle.setOpen(Double.parseDouble((String) item.get(1)));
	            candle.setHigh(Double.parseDouble((String) item.get(2)));
	            candle.setLow(Double.parseDouble((String) item.get(3)));
	            candle.setClose(Double.parseDouble((String) item.get(4)));
	            candle.setVolume(Double.parseDouble((String) item.get(5)));
	            result.add(candle);
	        }

	        return result;

	    } catch (Exception e) {
	        log.error("❌ Binance 캔들 조회 실패: {}", e.getMessage(), e);
	        return Collections.emptyList();
	    }
	}

}
