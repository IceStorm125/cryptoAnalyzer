package org.example.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.config.ApplicationSettings;

public class ClickHouseCandleRepository implements CandleRepository {

    private final ApplicationSettings.ClickHouseSettings settings;
    private final Connection connection;

    public ClickHouseCandleRepository(ApplicationSettings.ClickHouseSettings settings) throws IOException {
        if (settings == null) {
            throw new IllegalArgumentException("ClickHouse settings must not be null");
        }
        if (!isSafeIdentifier(settings.database())) {
            throw new IllegalArgumentException("ClickHouse database must contain only letters, digits and underscore");
        }

        this.settings = settings;
        this.connection = openConnection(settings);
        initializeSchema();
    }

    @Override
    public void saveHourlyCandles(Crypto asset, List<OhlcCandleDto> candles) throws IOException {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO %s
                (exchange, symbol, timeframe, ts, open, high, low, close, volume_base, volume_quote, ingested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(candlesTable());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp ingestedAt = Timestamp.from(Instant.now());
            for (OhlcCandleDto candle : candles) {
                if (candle == null) {
                    continue;
                }
                statement.setString(1, "cryptocompare");
                statement.setString(2, asset.getSymbol());
                statement.setString(3, "1h");
                statement.setTimestamp(4, Timestamp.from(Instant.ofEpochSecond(candle.getTime())));
                statement.setDouble(5, candle.getOpen());
                statement.setDouble(6, candle.getHigh());
                statement.setDouble(7, candle.getLow());
                statement.setDouble(8, candle.getClose());
                statement.setDouble(9, candle.getVolumefrom());
                statement.setDouble(10, candle.getVolumeto());
                statement.setTimestamp(11, ingestedAt);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IOException("Failed to save candles to ClickHouse for " + asset.getSymbol(), e);
        }
    }

    @Override
    public List<OhlcCandleDto> loadHourlyCandles(Crypto asset, int limit) throws IOException {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        String sql = """
                SELECT ts, open, high, low, close, volume_base, volume_quote
                FROM %s FINAL
                WHERE exchange = ? AND symbol = ? AND timeframe = ?
                ORDER BY ts DESC
                LIMIT ?
                """.formatted(candlesTable());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "cryptocompare");
            statement.setString(2, asset.getSymbol());
            statement.setString(3, "1h");
            statement.setInt(4, limit);

            List<OhlcCandleDto> candles = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    candles.add(new OhlcCandleDto(
                            resultSet.getTimestamp("ts").toInstant().getEpochSecond(),
                            resultSet.getDouble("open"),
                            resultSet.getDouble("high"),
                            resultSet.getDouble("low"),
                            resultSet.getDouble("close"),
                            resultSet.getDouble("volume_base"),
                            resultSet.getDouble("volume_quote")
                    ));
                }
            }
            Collections.reverse(candles);
            return Collections.unmodifiableList(candles);
        } catch (SQLException e) {
            throw new IOException("Failed to load candles from ClickHouse for " + asset.getSymbol(), e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close ClickHouse connection", e);
        }
    }

    private void initializeSchema() throws IOException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS " + settings.database());
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS %s
                    (
                        exchange LowCardinality(String),
                        symbol LowCardinality(String),
                        timeframe LowCardinality(String),
                        ts DateTime64(3, 'UTC'),
                        open Float64,
                        high Float64,
                        low Float64,
                        close Float64,
                        volume_base Float64,
                        volume_quote Float64,
                        ingested_at DateTime64(3, 'UTC')
                    )
                    ENGINE = ReplacingMergeTree(ingested_at)
                    PARTITION BY toYYYYMM(ts)
                    ORDER BY (exchange, symbol, timeframe, ts)
                    """.formatted(candlesTable()));
        } catch (SQLException e) {
            throw new IOException("Failed to initialize ClickHouse schema", e);
        }
    }

    private Connection openConnection(ApplicationSettings.ClickHouseSettings settings) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("user", settings.username() == null ? "default" : settings.username());
        properties.setProperty("password", settings.password() == null ? "" : settings.password());

        try {
            Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
            return DriverManager.getConnection(settings.url(), properties);
        } catch (ClassNotFoundException e) {
            throw new IOException("ClickHouse JDBC driver was not found", e);
        } catch (SQLException e) {
            throw new IOException("Failed to connect to ClickHouse: " + settings.url(), e);
        }
    }

    private boolean isSafeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private String candlesTable() {
        return settings.database() + ".candles";
    }
}
