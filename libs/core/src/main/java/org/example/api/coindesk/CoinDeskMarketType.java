package org.example.api.coindesk;

import lombok.Getter;

@Getter
public enum CoinDeskMarketType {

    SPOT("spot/v1/latest/tick"),
    FUTURES("futures/v1/latest/tick");

    private final String path;

    CoinDeskMarketType(String path) {
        this.path = path;
    }

}
