package org.example.strategy.ema_crossover;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.strategy.SignalType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmaCrossoverSignal {

    private long candleTime;
    private double closePrice;
    private double fastEma;
    private double slowEma;
    private double retestDistancePercent;
    private int candlesAfterCrossover;
    private SignalType signalType;
}
