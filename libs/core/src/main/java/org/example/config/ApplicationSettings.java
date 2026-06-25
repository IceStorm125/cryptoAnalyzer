package org.example.config;

import java.time.Duration;

public record ApplicationSettings(
        int candleLimit,
        boolean runInLoop,
        Duration pauseBetweenCycles,
        ClickHouseSettings clickHouse,
        RedisSettings redis,
        LokiSettings loki
) {

    public ApplicationSettings {
        if (candleLimit <= 0) {
            throw new IllegalArgumentException("Candle limit must be greater than 0");
        }
        if (pauseBetweenCycles == null || pauseBetweenCycles.isNegative() || pauseBetweenCycles.isZero()) {
            throw new IllegalArgumentException("Pause between cycles must be greater than 0");
        }
        if (clickHouse == null) {
            throw new IllegalArgumentException("ClickHouse settings must not be null");
        }
        if (redis == null) {
            throw new IllegalArgumentException("Redis settings must not be null");
        }
        if (loki == null) {
            throw new IllegalArgumentException("Loki settings must not be null");
        }
    }

    public record RedisSettings(
            boolean enabled,
            String host,
            int port,
            int database,
            String username,

            String password,
            String keyPrefix,
            long historyMaxLen
    ) {

        public RedisSettings {
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

    public record ClickHouseSettings(
            String url,
            String database,
            String username,
            String password
    ) {

        public ClickHouseSettings {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("ClickHouse url must not be blank");
            }
            if (database == null || database.isBlank()) {
                throw new IllegalArgumentException("ClickHouse database must not be blank");
            }
        }
    }

    public record LokiSettings(
            String url,
            String username,
            String password,
            String tenantId
    ) {

        public LokiSettings {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Loki url must not be blank");
            }
        }
    }
}
