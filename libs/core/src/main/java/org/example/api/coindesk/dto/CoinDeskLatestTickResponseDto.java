package org.example.api.coindesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class CoinDeskLatestTickResponseDto {

    @JsonProperty("Data")
    private Map<String, CoinDeskLatestTickDto> data;

    @JsonProperty("Err")
    private CoinDeskLatestTickErrorDto err;

    public Map<String, CoinDeskLatestTickDto> getData() {
        return data;
    }

    public CoinDeskLatestTickErrorDto getErr() {
        return err;
    }
}
