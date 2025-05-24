package com.auto.trader.indicator.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllComparisonResultDto {
	private Map<String, List<Map<String, Object>>> result;

	public static AllComparisonResultDto from(Map<String, List<?>> input) {
		Map<String, List<Map<String, Object>>> casted = new LinkedHashMap<>();

		for (Map.Entry<String, List<?>> entry : input.entrySet()) {
			List<Map<String, Object>> converted = new ArrayList<>();
			for (Object item : entry.getValue()) {
				if (item instanceof Map<?, ?> map) {
					Map<String, Object> safe = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : map.entrySet()) {
						safe.put(String.valueOf(e.getKey()), e.getValue());
					}
					converted.add(safe);
				}
			}
			casted.put(entry.getKey(), converted);
		}

		return new AllComparisonResultDto(casted);
	}
}
