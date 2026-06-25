package org.example.api.coindesk;

public record CoinDeskAssetPrice(
        CoinDeskMarketType marketType,
        String market,
        String instrument,
        double value,
        Long valueLastUpdateTimestamp,
        Double bestBid,
        Long bestBidLastUpdateTimestamp,
        Double bestAsk,
        Long bestAskLastUpdateTimestamp
) {
}
