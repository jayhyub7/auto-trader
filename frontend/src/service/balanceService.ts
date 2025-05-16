import api from "@/shared/util/axios";

export type BalanceItem = {
  asset: string;
  available: number;
  locked: number;
  total: number;
  usdValue: number;
  krwValue: number;
};

export type ExchangeBalance = {
  exchange: string;
  balances: BalanceItem[];
  totalUsdValue: number;
  validated: boolean;
};

export const fetchBalances = async (): Promise<Record<string, ExchangeBalance>> => {
  const res = await api.get<ExchangeBalance[]>("/current-balance");

  return res.data.reduce((acc, cur) => {
    acc[cur.exchange] = cur;
    return acc;
  }, {} as Record<string, ExchangeBalance>);
};
