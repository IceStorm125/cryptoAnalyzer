package org.example.detector;

import java.util.List;
import org.example.api.cryptocompare.Crypto;

public record DetectorResult<T>(
        String detectorName,
        Crypto asset,
        int candlesAnalyzed,
        List<T> detections
) {

    public DetectorResult {
        if (detectorName == null || detectorName.isBlank()) {
            throw new IllegalArgumentException("Detector name must not be blank");
        }
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (candlesAnalyzed < 0) {
            throw new IllegalArgumentException("Candles analyzed must not be negative");
        }
        if (detections == null) {
            throw new IllegalArgumentException("Detections must not be null");
        }

        detections = List.copyOf(detections);
    }

    public int detectionCount() {
        return detections.size();
    }
}
