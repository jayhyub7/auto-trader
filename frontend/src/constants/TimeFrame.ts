// src/constants/timeframes.ts

export enum Timeframe {
  ONE_MINUTE = "1m",
  THREE_MINUTES = "3m",
  FIVE_MINUTES = "5m",
  FIFTEEN_MINUTES = "15m",
  ONE_HOUR = "1h",
  FOUR_HOURS = "4h",
  ONE_DAY = "1d",
  ONE_WEEK = "1w",
  ONE_MONTH = "1M",
}

export const TIMEFRAME_LABELS: Record<Timeframe, string> = {
  [Timeframe.ONE_MINUTE]: "1m",
  [Timeframe.THREE_MINUTES]: "3m",
  [Timeframe.FIVE_MINUTES]: "5m",
  [Timeframe.FIFTEEN_MINUTES]: "15m",
  [Timeframe.ONE_HOUR]: "1h",
  [Timeframe.FOUR_HOURS]: "4h",
  [Timeframe.ONE_DAY]: "1d",
  [Timeframe.ONE_WEEK]: "1w",
  [Timeframe.ONE_MONTH]: "1M",
};
