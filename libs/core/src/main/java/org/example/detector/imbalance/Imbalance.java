package org.example.detector.imbalance;

import java.time.Instant;

public record Imbalance(
        long createdAtTime,
        double lowerBound,
        double upperBound,
        ImbalanceType type
) {

    public Imbalance {
        if (upperBound <= lowerBound) {
            throw new IllegalArgumentException("Upper bound must be greater than lower bound");
        }
        if (type == null) {
            throw new IllegalArgumentException("Imbalance type must not be null");
        }
    }

    @Override
    public String toString() {
        return "Imbalance[createdAtTime=" + createdAtTime
                + " (" + Instant.ofEpochSecond(createdAtTime) + ")"
                + ", lowerBound=" + lowerBound
                + ", upperBound=" + upperBound
                + ", type=" + type
                + "]";
    }
}
