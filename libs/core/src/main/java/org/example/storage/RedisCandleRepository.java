package org.example.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.config.ApplicationSettings;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

public class RedisCandleRepository implements CandleRepository {

    private final ApplicationSettings.RedisSettings settings;
    private final ObjectMapper objectMapper;
    private final JedisPooled jedis;

    public RedisCandleRepository(ApplicationSettings.RedisSettings settings) {
        this(settings, defaultObjectMapper());
    }

    public RedisCandleRepository(ApplicationSettings.RedisSettings settings, ObjectMapper objectMapper) {
        if (settings == null) {
            throw new IllegalArgumentException("Redis settings must not be null");
        }
        if (!settings.enabled()) {
            throw new IllegalArgumentException("Redis must be enabled for candle storage");
        }
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }

        this.settings = settings;
        this.objectMapper = objectMapper;

        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .database(settings.database());

        if (settings.username() != null && !settings.username().isBlank()) {
            clientConfigBuilder.user(settings.username());
        }
        if (settings.password() != null && !settings.password().isBlank()) {
            clientConfigBuilder.password(settings.password());
        }

        this.jedis = new JedisPooled(new HostAndPort(settings.host(), settings.port()), clientConfigBuilder.build());
    }

    @Override
    public void saveHourlyCandles(Crypto asset, List<OhlcCandleDto> candles) throws IOException {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }

        String key = candlesKey(asset);
        for (OhlcCandleDto candle : candles) {
            if (candle == null) {
                continue;
            }
            String candleTime = Long.toString(candle.getTime());
            jedis.zadd(key, candle.getTime(), candleTime);
            jedis.hset(payloadsKey(asset), candleTime, serialize(candle));
        }
        jedis.hset(metaKey(asset), "latest_ingested_at_epoch_ms", Long.toString(System.currentTimeMillis()));
        jedis.hset(metaKey(asset), "asset", asset.getSymbol());
        jedis.hset(metaKey(asset), "candle_count", Long.toString(jedis.zcard(key)));
    }

    @Override
    public List<OhlcCandleDto> loadHourlyCandles(Crypto asset, int limit) throws IOException {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        List<String> candleTimes = new ArrayList<>(jedis.zrevrange(candlesKey(asset), 0, limit - 1));
        Collections.reverse(candleTimes);

        List<OhlcCandleDto> candles = new ArrayList<>(candleTimes.size());
        for (String candleTime : candleTimes) {
            String payload = jedis.hget(payloadsKey(asset), candleTime);
            if (payload != null) {
                candles.add(objectMapper.readValue(payload, OhlcCandleDto.class));
            }
        }

        return Collections.unmodifiableList(candles);
    }

    @Override
    public void close() {
        jedis.close();
    }

    private String serialize(OhlcCandleDto candle) throws JsonProcessingException {
        return objectMapper.writeValueAsString(candle);
    }

    private String candlesKey(Crypto asset) {
        return key("candles", "hourly", asset.getSymbol());
    }

    private String payloadsKey(Crypto asset) {
        return key("candles", "hourly", asset.getSymbol(), "payloads");
    }

    private String metaKey(Crypto asset) {
        return key("candles", "meta", asset.getSymbol());
    }

    private String key(String... parts) {
        return settings.keyPrefix() + ":" + String.join(":", parts);
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
