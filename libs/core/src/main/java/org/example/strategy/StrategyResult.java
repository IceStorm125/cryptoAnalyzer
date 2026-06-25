package org.example.strategy;

import java.util.List;
import org.example.api.cryptocompare.Crypto;

public record StrategyResult(
        String strategyName,
        Crypto asset,
        int candlesAnalyzed,
        List<String> signalDescriptions
) {

    public StrategyResult {
        if (strategyName == null || strategyName.isBlank()) {
            throw new IllegalArgumentException("Strategy name must not be blank");
        }
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (candlesAnalyzed < 0) {
            throw new IllegalArgumentException("Candles analyzed must not be negative");
        }
        if (signalDescriptions == null) {
            throw new IllegalArgumentException("Signal descriptions must not be null");
        }

        signalDescriptions = List.copyOf(signalDescriptions);
    }

    public int signalCount() {
        return signalDescriptions.size();
    }
}
