package com.auto.trader.trade.indicator;

public enum CandlePattern {
	NONE, // 판별된 패턴 없음
	HAMMER, // 아래꼬리 긴 반전형 양봉
	SHOOTING_STAR, // 윗꼬리 긴 반전형 음봉
	BULLISH_ENGULFING, // 전 캔들을 완전히 덮는 양봉
	BEARISH_ENGULFING // 전 캔들을 완전히 덮는 음봉
}