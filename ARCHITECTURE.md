# Crypto Analyzer Architecture

The project is organized as a monorepo with two independent services and one shared module:

```text
libs/
  core/                         # shared API clients, strategies, detectors, storage adapters
services/
  price-ingestion/              # loads candles from external APIs and writes them to ClickHouse
  strategy-backtester/          # reads candles from ClickHouse and runs strategies
```

## Services

`price-ingestion`:

- entrypoint: `org.example.ingestion.PriceIngestionMain`
- loads hourly candles through `CryptoCompareHttpClient`
- stores candles through `ClickHouseCandleRepository`
- runs in a loop by default

`strategy-backtester`:

- entrypoint: `org.example.backtester.StrategyBacktesterMain`
- reads hourly candles through `ClickHouseCandleRepository`
- runs enabled strategies and detectors
- publishes analysis results to logs and Redis
- runs once by default

## Storage

Historical candles are stored in ClickHouse:

```text
database: crypto_analyzer
table: candles
```

The table is created automatically when ingestion or backtester starts:

```sql
ENGINE = ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(ts)
ORDER BY (exchange, symbol, timeframe, ts)
```

Redis remains useful for analysis result publishing, current statuses, Grafana, and Redis Insight.

## Run

Build all modules:

```bash
mvn package -DskipTests
```

Run infrastructure and services:

```bash
docker compose up --build
```

Run only ingestion:

```bash
docker compose up --build price-ingestion
```

Run the backtester after data has been ingested:

```bash
docker compose up --build strategy-backtester
```

Settings from `application.properties` can be overridden through environment variables:

```text
CLICKHOUSE_URL=jdbc:clickhouse://clickhouse:8123
CLICKHOUSE_DATABASE=crypto_analyzer
CLICKHOUSE_USERNAME=cryptoanalyzer
CLICKHOUSE_PASSWORD=cryptoanalyzer123
REDIS_HOST=redis
ANALYSIS_RUN_IN_LOOP=false
LOGGING_LOKI_URL=http://loki:3100/loki/api/v1/push
```
