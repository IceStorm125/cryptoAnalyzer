package org.example.redis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.example.analysis.publish.AnalysisResultPublisher;
import org.example.api.cryptocompare.Crypto;
import org.example.detector.DetectorResult;
import org.example.detector.imbalance.Imbalance;
import org.example.detector.order_block.OrderBlock;
import org.example.strategy.StrategyResult;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;

public class RedisAnalysisResultPublisher implements AnalysisResultPublisher {

    private final RedisPublisherSettings settings;
    private final JedisPooled jedis;

    public RedisAnalysisResultPublisher(RedisPublisherSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("RedisPublisherSettings must not be null");
        }

        this.settings = settings;
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
    public void publishRunStarted(List<Crypto> assets, int candleLimit) {
        long now = System.currentTimeMillis();
        String assetSymbols = assets.stream()
                .map(Crypto::getSymbol)
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        jedis.del(grafanaTableKey());
        jedis.hset(key("run", "meta"), Map.of(
                "status", "running",
                "started_at_epoch_ms", Long.toString(now),
                "updated_at_epoch_ms", Long.toString(now),
                "candle_limit", Integer.toString(candleLimit),
                "assets", assetSymbols
        ));
    }

    @Override
    public void publishRunCompleted() {
        long now = System.currentTimeMillis();
        jedis.hset(key("run", "meta"), Map.of(
                "status", "idle",
                "finished_at_epoch_ms", Long.toString(now),
                "updated_at_epoch_ms", Long.toString(now)
        ));
    }

    @Override
    public void publishAssetStarted(Crypto asset, int candlesLoaded) {
        long now = System.currentTimeMillis();
        jedis.hset(currentAssetKey(asset), Map.of(
                "asset", asset.getSymbol(),
                "candles_loaded", Integer.toString(candlesLoaded),
                "status", "running",
                "updated_at_epoch_ms", Long.toString(now)
        ));
        jedis.hdel(currentAssetKey(asset), "last_error");
    }

    @Override
    public void publishStrategyResult(StrategyResult result) {
        String strategySlug = slugify(result.strategyName());
        long now = System.currentTimeMillis();

        jedis.hset(currentAssetKey(result.asset()), Map.of(
                "strategy." + strategySlug + ".signal_count", Integer.toString(result.signalCount()),
                "strategy." + strategySlug + ".updated_at_epoch_ms", Long.toString(now),
                "updated_at_epoch_ms", Long.toString(now)
        ));

        appendHistory(
                result.asset(),
                "strategy",
                strategySlug,
                Map.of("signal_count", Integer.toString(result.signalCount()))
        );
    }

    @Override
    public void publishDetectorResult(DetectorResult<?> result) {
        String detectorSlug = slugify(result.detectorName());
        long now = System.currentTimeMillis();
        Map<String, String> currentFields = new LinkedHashMap<>();
        currentFields.put("detector." + detectorSlug + ".count", Integer.toString(result.detectionCount()));
        currentFields.put("detector." + detectorSlug + ".updated_at_epoch_ms", Long.toString(now));
        currentFields.put("updated_at_epoch_ms", Long.toString(now));

        List<DetectionSnapshot> snapshots = buildSnapshots(result, detectorSlug, now, currentFields);

        jedis.hset(currentAssetKey(result.asset()), currentFields);
        syncActiveDetections(result.asset(), detectorSlug, snapshots);
        appendHistory(result.asset(), "detector", detectorSlug, historyFields(result, currentFields));
    }

    @Override
    public void publishAssetCompleted(Crypto asset) {
        long now = System.currentTimeMillis();
        Map<String, String> currentFields = jedis.hgetAll(currentAssetKey(asset));
        jedis.hset(currentAssetKey(asset), Map.of(
                "status", "idle",
                "updated_at_epoch_ms", Long.toString(now)
        ));

        Map<String, String> tableFields = new LinkedHashMap<>();
        tableFields.put("asset", asset.getSymbol());
        tableFields.put("imbalance_count", currentFields.getOrDefault("detector.imbalance_detector.count", "0"));
        tableFields.put("order_block_count", currentFields.getOrDefault("detector.order_block_detector.count", "0"));
        tableFields.put("time", Instant.ofEpochMilli(now).toString());
        jedis.xadd(grafanaTableKey(), StreamEntryID.NEW_ENTRY, tableFields);
    }

    @Override
    public void publishAssetFailed(Crypto asset, Exception exception) {
        long now = System.currentTimeMillis();
        jedis.hset(currentAssetKey(asset), Map.of(
                "status", "failed",
                "last_error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                "updated_at_epoch_ms", Long.toString(now)
        ));
    }

    @Override
    public void close() {
        jedis.close();
    }

    private List<DetectionSnapshot> buildSnapshots(
            DetectorResult<?> result,
            String detectorSlug,
            long now,
            Map<String, String> currentFields
    ) {
        List<DetectionSnapshot> snapshots = new ArrayList<>();
        int bullishCount = 0;
        int bearishCount = 0;

        for (Object detection : result.detections()) {
            DetectionSnapshot snapshot = buildDetectionSnapshot(result.asset(), detectorSlug, detection, now);
            snapshots.add(snapshot);

            if ("BULLISH".equals(snapshot.type())) {
                bullishCount++;
            }
            if ("BEARISH".equals(snapshot.type())) {
                bearishCount++;
            }
        }

        currentFields.put("detector." + detectorSlug + ".bullish_count", Integer.toString(bullishCount));
        currentFields.put("detector." + detectorSlug + ".bearish_count", Integer.toString(bearishCount));

        return snapshots;
    }

    private DetectionSnapshot buildDetectionSnapshot(
            Crypto asset,
            String detectorSlug,
            Object detection,
            long now
    ) {
        if (detection instanceof Imbalance imbalance) {
            return new DetectionSnapshot(
                    buildRangeId(imbalance.type().name(), imbalance.createdAtTime(), imbalance.lowerBound(), imbalance.upperBound()),
                    imbalance.type().name(),
                    detailKey(asset, detectorSlug, buildRangeId(
                            imbalance.type().name(),
                            imbalance.createdAtTime(),
                            imbalance.lowerBound(),
                            imbalance.upperBound()
                    )),
                    buildRangeFields(
                            asset,
                            detectorSlug,
                            "Imbalance",
                            imbalance.type().name(),
                            imbalance.createdAtTime(),
                            imbalance.lowerBound(),
                            imbalance.upperBound(),
                            now,
                            detection.toString()
                    )
            );
        }

        if (detection instanceof OrderBlock orderBlock) {
            return new DetectionSnapshot(
                    buildRangeId(orderBlock.type().name(), orderBlock.createdAtTime(), orderBlock.lowerBound(), orderBlock.upperBound()),
                    orderBlock.type().name(),
                    detailKey(asset, detectorSlug, buildRangeId(
                            orderBlock.type().name(),
                            orderBlock.createdAtTime(),
                            orderBlock.lowerBound(),
                            orderBlock.upperBound()
                    )),
                    buildRangeFields(
                            asset,
                            detectorSlug,
                            "OrderBlock",
                            orderBlock.type().name(),
                            orderBlock.createdAtTime(),
                            orderBlock.lowerBound(),
                            orderBlock.upperBound(),
                            now,
                            detection.toString()
                    )
            );
        }

        String fallbackId = Integer.toHexString(Objects.hash(detectorSlug, detection.toString()));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("asset", asset.getSymbol());
        fields.put("detector", detectorSlug);
        fields.put("detection_id", fallbackId);
        fields.put("detection_class", detection.getClass().getSimpleName());
        fields.put("payload", detection.toString());
        fields.put("updated_at_epoch_ms", Long.toString(now));

        return new DetectionSnapshot(
                fallbackId,
                detection.getClass().getSimpleName(),
                detailKey(asset, detectorSlug, fallbackId),
                fields
        );
    }

    private Map<String, String> buildRangeFields(
            Crypto asset,
            String detectorSlug,
            String detectionClass,
            String type,
            long createdAtTime,
            double lowerBound,
            double upperBound,
            long now,
            String payload
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("asset", asset.getSymbol());
        fields.put("detector", detectorSlug);
        fields.put("detection_class", detectionClass);
        fields.put("detection_type", type);
        fields.put("detection_id", buildRangeId(type, createdAtTime, lowerBound, upperBound));
        fields.put("created_at_epoch_sec", Long.toString(createdAtTime));
        fields.put("created_at_utc", Instant.ofEpochSecond(createdAtTime).toString());
        fields.put("lower_bound", Double.toString(lowerBound));
        fields.put("upper_bound", Double.toString(upperBound));
        fields.put("status", "untested");
        fields.put("updated_at_epoch_ms", Long.toString(now));
        fields.put("payload", payload);
        return fields;
    }

    private Map<String, String> historyFields(DetectorResult<?> result, Map<String, String> currentFields) {
        Map<String, String> historyFields = new LinkedHashMap<>();
        historyFields.put("count", Integer.toString(result.detectionCount()));

        String detectorSlug = slugify(result.detectorName());
        String bullishKey = "detector." + detectorSlug + ".bullish_count";
        String bearishKey = "detector." + detectorSlug + ".bearish_count";

        if (currentFields.containsKey(bullishKey)) {
            historyFields.put("bullish_count", currentFields.get(bullishKey));
        }
        if (currentFields.containsKey(bearishKey)) {
            historyFields.put("bearish_count", currentFields.get(bearishKey));
        }

        return historyFields;
    }

    private void syncActiveDetections(Crypto asset, String detectorSlug, List<DetectionSnapshot> snapshots) {
        String activeSetKey = activeDetectionsKey(asset, detectorSlug);
        Set<String> existingIds = jedis.smembers(activeSetKey);
        Set<String> newIds = new LinkedHashSet<>();

        for (DetectionSnapshot snapshot : snapshots) {
            newIds.add(snapshot.id());
            jedis.hset(snapshot.key(), snapshot.fields());
        }

        for (String existingId : existingIds) {
            if (!newIds.contains(existingId)) {
                jedis.del(detailKey(asset, detectorSlug, existingId));
            }
        }

        jedis.del(activeSetKey);
        if (!newIds.isEmpty()) {
            jedis.sadd(activeSetKey, newIds.toArray(String[]::new));
        }
    }

    private void appendHistory(Crypto asset, String category, String name, Map<String, String> values) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("asset", asset.getSymbol());
        fields.put("category", category);
        fields.put("name", name);
        fields.put("timestamp_epoch_ms", Long.toString(System.currentTimeMillis()));
        fields.putAll(values);

        String historyKey = key("history", asset.getSymbol(), category, name);
        jedis.xadd(historyKey, StreamEntryID.NEW_ENTRY, fields);
        jedis.xtrim(historyKey, settings.historyMaxLen(), true);
    }

    private String currentAssetKey(Crypto asset) {
        return key("current", asset.getSymbol());
    }

    private String activeDetectionsKey(Crypto asset, String detectorSlug) {
        return key("active", asset.getSymbol(), detectorSlug);
    }

    private String grafanaTableKey() {
        return key("grafana", "table");
    }

    private String detailKey(Crypto asset, String detectorSlug, String detectionId) {
        return key("detection", asset.getSymbol(), detectorSlug, detectionId);
    }

    private String key(String... parts) {
        return settings.keyPrefix() + ":" + String.join(":", parts);
    }

    private String buildRangeId(String type, long createdAtTime, double lowerBound, double upperBound) {
        return type + ":" + createdAtTime + ":" + Double.toString(lowerBound) + ":" + Double.toString(upperBound);
    }

    private String slugify(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private record DetectionSnapshot(
            String id,
            String type,
            String key,
            Map<String, String> fields
    ) {
    }
}
