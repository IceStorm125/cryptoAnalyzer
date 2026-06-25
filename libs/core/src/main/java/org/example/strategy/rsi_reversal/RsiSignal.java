package org.example.strategy.rsi_reversal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.strategy.SignalType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RsiSignal {

    private long candleTime;
    private double closePrice;
    private double rsi;
    private SignalType signalType;
}
