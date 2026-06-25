package org.example.api.cryptocompare.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OhlcCandleDto {

    private long time;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volumefrom;
    private double volumeto;
}
