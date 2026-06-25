package org.example.api.cryptocompare;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Crypto {
    BTC("BTC", "BTC-USDT", "BTC-USDT-VANILLA-PERPETUAL"),
    ETH("ETH", "ETH-USDT", "ETH-USDT-VANILLA-PERPETUAL"),
    SOL("SOL", "SOL-USDT", "SOL-USDT-VANILLA-PERPETUAL"),
    XRP("XRP", "XRP-USDT", "XRP-USDT-VANILLA-PERPETUAL"),
    ADA("ADA", "ADA-USDT", "ADA-USDT-VANILLA-PERPETUAL"),
    BNB("BNB", "BNB-USDT", "BNB-USDT-VANILLA-PERPETUAL"),
    DOGE("DOGE", "DOGE-USDT", "DOGE-USDT-VANILLA-PERPETUAL"),
    TRX("TRX", "TRX-USDT", "TRX-USDT-VANILLA-PERPETUAL"),
    LINK("LINK", "LINK-USDT", "LINK-USDT-VANILLA-PERPETUAL"),
    AVAX("AVAX", "AVAX-USDT", "AVAX-USDT-VANILLA-PERPETUAL"),
    LTC("LTC", "LTC-USDT", "LTC-USDT-VANILLA-PERPETUAL"),
    DOT("DOT", "DOT-USDT", "DOT-USDT-VANILLA-PERPETUAL"),
    SUI("SUI", "SUI-USDT", "SUI-USDT-VANILLA-PERPETUAL"),
    BCH("BCH", "BCH-USDT", "BCH-USDT-VANILLA-PERPETUAL"),
    XLM("XLM", "XLM-USDT", "XLM-USDT-VANILLA-PERPETUAL");

    private final String symbol;
    private final String coinDeskSpotInstrument;
    private final String coinDeskFuturesInstrument;
}
