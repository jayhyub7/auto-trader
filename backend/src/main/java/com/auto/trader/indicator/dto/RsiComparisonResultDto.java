// 파일: com.auto.trader.indicator.dto.RsiComparisonResultDto.java

package com.auto.trader.indicator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RsiComparisonResultDto {
	private long time;
	private double frontendRsi;
	private double backendRsi;
	private double diff;
}
