package com.auto.trader.balance.dto;

import java.util.List;

public record ExchangeBalanceDto(
    String exchange,
    boolean validated,
    double totalUsdValue,
    List<BalanceDto> balances
) {}