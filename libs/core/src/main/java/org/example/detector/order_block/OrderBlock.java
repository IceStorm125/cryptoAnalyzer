package org.example.detector.order_block;

import java.time.Instant;

public record OrderBlock(
        long createdAtTime,
        double lowerBound,
        double upperBound,
        OrderBlockType type
) {

    public OrderBlock {
        if (upperBound <= lowerBound) {
            throw new IllegalArgumentException("Upper bound must be greater than lower bound");
        }
        if (type == null) {
            throw new IllegalArgumentException("Order block type must not be null");
        }
    }

    @Override
    public String toString() {
        return "OrderBlock[createdAtTime=" + createdAtTime
                + " (" + Instant.ofEpochSecond(createdAtTime) + ")"
                + ", lowerBound=" + lowerBound
                + ", upperBound=" + upperBound
                + ", type=" + type
                + "]";
    }
}
