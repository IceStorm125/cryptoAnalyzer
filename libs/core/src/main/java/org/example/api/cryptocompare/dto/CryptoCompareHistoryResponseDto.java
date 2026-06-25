package org.example.api.cryptocompare.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoCompareHistoryResponseDto {

    @JsonProperty("Response")
    private String response;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Type")
    private int type;

    @JsonProperty("Data")
    private DataContainerDto data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataContainerDto {

        @JsonProperty("Aggregated")
        private boolean aggregated;

        @JsonProperty("TimeFrom")
        private long timeFrom;

        @JsonProperty("TimeTo")
        private long timeTo;

        @JsonProperty("Data")
        private List<OhlcCandleDto> data;

    }
}
