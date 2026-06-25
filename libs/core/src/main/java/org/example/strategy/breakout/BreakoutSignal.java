package org.example.strategy.breakout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.strategy.SignalType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BreakoutSignal {

    private long candleTime;
    private double closePrice;
    private double breakoutLevel;
    private int lookbackPeriod;
    private SignalType signalType;
}
