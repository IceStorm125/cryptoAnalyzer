package org.example.redis;

public record RedisPublisherSettings(
        String host,
        int port,
        int database,
        String username,
        String password,
        String keyPrefix,
        long historyMaxLen
) {

    public RedisPublisherSettings {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Redis host must not be blank");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("Redis port must be greater than 0");
        }
        if (database < 0) {
            throw new IllegalArgumentException("Redis database must not be negative");
        }
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("Redis key prefix must not be blank");
        }
        if (historyMaxLen <= 0) {
            throw new IllegalArgumentException("Redis history max len must be greater than 0");
        }
    }
}
