package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public final class ApplicationSettingsLoader {

    private static final String SETTINGS_RESOURCE = "application.properties";

    private ApplicationSettingsLoader() {
    }

    public static ApplicationSettings load() throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = ApplicationSettingsLoader.class.getClassLoader()
                .getResourceAsStream(SETTINGS_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + SETTINGS_RESOURCE);
            }
            properties.load(inputStream);
        }

        return new ApplicationSettings(
                Integer.parseInt(readRequired(properties, "analysis.candle-limit")),
                Boolean.parseBoolean(readOrDefault(properties, "analysis.run-in-loop", "true")),
                Duration.ofSeconds(Long.parseLong(readOrDefault(properties, "analysis.pause-seconds", "30"))),
                new ApplicationSettings.ClickHouseSettings(
                        readOrDefault(properties, "clickhouse.url", "jdbc:clickhouse://localhost:8123"),
                        readOrDefault(properties, "clickhouse.database", "crypto_analyzer"),
                        readOrDefault(properties, "clickhouse.username", "default"),
                        readOrDefault(properties, "clickhouse.password", "")
                ),
                new ApplicationSettings.RedisSettings(
                        Boolean.parseBoolean(readOrDefault(properties, "redis.enabled", "false")),
                        readOrDefault(properties, "redis.host", "localhost"),
                        Integer.parseInt(readOrDefault(properties, "redis.port", "6379")),
                        Integer.parseInt(readOrDefault(properties, "redis.database", "0")),
                        emptyToNull(readOrDefault(properties, "redis.username", "")),
                        emptyToNull(readOrDefault(properties, "redis.password", "")),
                        readOrDefault(properties, "redis.key-prefix", "analysis"),
                        Long.parseLong(readOrDefault(properties, "redis.history-max-len", "2000"))
                ),
                new ApplicationSettings.LokiSettings(
                        readOrDefault(properties, "logging.loki.url", "http://localhost:3100/loki/api/v1/push"),
                        emptyToNull(readOrDefault(properties, "logging.loki.username", "")),
                        emptyToNull(readOrDefault(properties, "logging.loki.password", "")),
                        emptyToNull(readOrDefault(properties, "logging.loki.tenant-id", ""))
                )
        );
    }

    private static String readRequired(Properties properties, String key) {
        String value = readOverride(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return value.trim();
    }

    private static String readOrDefault(Properties properties, String key, String defaultValue) {
        String value = readOverride(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String readOverride(String key) {
        String systemPropertyValue = System.getProperty(key);
        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return systemPropertyValue;
        }

        String envKey = key.toUpperCase()
                .replace('.', '_')
                .replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return null;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
