package org.example.api.coindesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CoinDeskLatestTickDto {

    @JsonProperty("TYPE")
    private String type;

    @JsonProperty("MARKET")
    private String market;

    @JsonProperty("INSTRUMENT")
    private String instrument;

    @JsonProperty("PRICE")
    private Double price;

    @JsonProperty("PRICE_LAST_UPDATE_TS")
    private Long priceLastUpdateTimestamp;

    @JsonProperty("BEST_BID")
    private Double bestBid;

    @JsonProperty("BEST_BID_LAST_UPDATE_TS")
    private Long bestBidLastUpdateTimestamp;

    @JsonProperty("BEST_ASK")
    private Double bestAsk;

    @JsonProperty("BEST_ASK_LAST_UPDATE_TS")
    private Long bestAskLastUpdateTimestamp;

}
