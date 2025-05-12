package com.auto.trader.balance.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeBalanceDto {
  private String exchange;
  private boolean validated;
  private double totalUsdValue;
  private List<BalanceDto> balances;
}
