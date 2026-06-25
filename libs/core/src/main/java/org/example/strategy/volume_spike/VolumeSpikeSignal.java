package org.example.strategy.volume_spike;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.strategy.SignalType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolumeSpikeSignal {

    private long candleTime;
    private double closePrice;
    private double volumeTo;
    private double baselineVolumeTo;
    private double volumeRatio;
    private SignalType signalType;
}
