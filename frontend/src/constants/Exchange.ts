// src/constants/Exchange.ts

export enum Exchange {
    BINANCE = "BINANCE",
    BITGET = "BITGET",
    BYBIT = "BYBIT",
  }
  
  export const EXCHANGE_LABELS: Record<Exchange, string> = {
    [Exchange.BINANCE]: "Binance",
    [Exchange.BITGET]: "Bitget",
    [Exchange.BYBIT]: "Bybit",
  };
  