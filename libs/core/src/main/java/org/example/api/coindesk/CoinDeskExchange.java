package org.example.api.coindesk;

import lombok.Getter;

@Getter
public enum CoinDeskExchange {

    BINANCE("binance"),
    BYBIT("bybit"),
    OKX("okx"),
    KRAKEN("kraken"),
    BITGET("bitget");

    private final String market;

    CoinDeskExchange(String market) {
        this.market = market;
    }

}
