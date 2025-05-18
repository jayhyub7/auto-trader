package com.auto.trader.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // ✅ 기본 생성자 추가
@AllArgsConstructor // ✅ 모든 필드 포함 생성자도 같이
public class CandleDto {
	private long time;
	private double open;
	private double high;
	private double low;
	private double close;
	private double volume;
	private boolean isFinal;
}
