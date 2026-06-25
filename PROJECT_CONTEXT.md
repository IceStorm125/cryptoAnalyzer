# Crypto Analyzer Project Context

## Purpose

This project is an experimental Java application for finding crypto chart setups and market structure objects.

Current goals:
- load historical OHLC candles for selected crypto assets
- run trading strategies over those candles
- run market structure detectors over the same candles
- publish findings both to the console and to Redis
- expose a Grafana-friendly current snapshot for monitoring

This is still a research / experimentation codebase, not an execution engine.

## Current Architecture

The codebase is currently split into these main areas:

1. `analysis`
2. `strategy`
3. `detector`
4. `api`
5. `config`
6. `redis`

External API integrations live under `api` and are split by provider:
- `api/cryptocompare`
- `api/coindesk`

There is also a result publishing layer under:
- `analysis/publish`

## Runtime Model

The entry point is:
`src/main/java/org/example/Main.java`

`Main` is intentionally still the place where the user enables:
- selected assets
- enabled strategies
- enabled detectors

Runtime settings that are not part of the experiment selection itself now live in:
- `src/main/resources/application.properties`

That file currently controls:
- candle limit
- whether the app runs once or in a loop
- pause between cycles
- Redis connection settings

## Current Runtime Configuration

### In `Main`

Assets are currently selected directly in `Main` from `Crypto`.

At the moment the active assets are:
- `BTC`
- `ETH`
- `SOL`
- `XRP`

Enabled strategies are currently:
- `VolumeSpikeAnalysisStrategy`

Enabled detectors are currently:
- `ImbalanceDetector`
- `OrderBlockDetector`

### In `application.properties`

Current runtime config file:
`src/main/resources/application.properties`

It currently contains:
- `analysis.candle-limit=2000`
- `analysis.run-in-loop=true`
- `analysis.pause-seconds=30`
- Redis connection settings including ACL username/password

## Runtime Flow

Runtime flow is now:

1. `Main` loads `ApplicationSettings` from `application.properties`.
2. `Main` selects assets and enabled components.
3. `AnalysisApplication` receives:
   - `CryptoCompareHttpClient`
   - candle limit
   - enabled strategies
   - enabled detectors
   - `AnalysisResultPublisher`
4. For each cycle, each asset is processed independently.
5. Candles are loaded once per asset.
6. The same candle list is passed to all strategies.
7. The same candle list is passed to all detectors.
8. Results are published to console and optionally to Redis.
9. If loop mode is enabled, the app sleeps for the configured pause and repeats.

Important:
- candles are loaded once per asset
- candles are not reloaded separately per strategy
- candles are not reloaded separately per detector
- one bad asset response must not stop processing of other assets
- one full loop is treated as a current snapshot

## Main Components

### 1. Entry Point

File:
`src/main/java/org/example/Main.java`

Responsibility:
- load runtime settings from `ApplicationSettingsLoader`
- select assets from `Crypto`
- choose enabled strategies
- choose enabled detectors
- build result publishers
- start one-shot or loop execution

### 2. Application Orchestration

File:
`src/main/java/org/example/analysis/AnalysisApplication.java`

Responsibility:
- run one cycle via `runOnce(...)`
- run forever with pause via `runInLoop(...)`
- iterate through assets
- request candles once per asset
- run strategies on the loaded candles
- run detectors on the same loaded candles
- publish results through `AnalysisResultPublisher`
- continue after asset-level API failures

Rules:
- zero strategies is allowed
- zero detectors is allowed
- both lists cannot be empty at the same time
- loop pause must be greater than zero

### 3. Publishing Layer

Main package:
`src/main/java/org/example/analysis/publish`

Key types:
- `AnalysisResultPublisher`
- `ConsoleAnalysisResultPublisher`
- `CompositeAnalysisResultPublisher`

Responsibility:
- decouple analysis from output/storage
- support multiple outputs for the same run
- keep `AnalysisApplication` free from direct `System.out` / Redis details

### 4. Redis Integration

Main package:
`src/main/java/org/example/redis`

Key files:
- `RedisAnalysisResultPublisher`
- `RedisPublisherSettings`

Responsibility:
- connect to Redis using configured ACL username/password
- publish current per-asset detector state
- publish active detection details
- publish detector/strategy history streams
- publish a Grafana-oriented table snapshot

### 5. Market Data Client

File:
`src/main/java/org/example/api/cryptocompare/CryptoCompareHttpClient.java`

Responsibility:
- call CryptoCompare `histohour`
- deserialize payload
- validate the response structure
- return immutable candle lists

Behavior:
- if CryptoCompare returns invalid payload, `IOException` is thrown with diagnostic context
- this was added because the API can return responses where expected nested `Data` is missing

### 6. Application Config

Main package:
`src/main/java/org/example/config`

Key files:
- `ApplicationSettings`
- `ApplicationSettingsLoader`

Responsibility:
- load runtime settings from `application.properties`
- validate that the settings are structurally correct

## Strategy Layer

The strategy layer is for setup / signal generation.

Key contract:
`src/main/java/org/example/strategy/AnalysisStrategy.java`

```java
String getName();
StrategyResult analyze(Crypto asset, List<OhlcCandleDto> candles);
```

Normalized result:
`src/main/java/org/example/strategy/StrategyResult.java`

Each strategy package should ideally contain:
- core strategy logic
- settings
- adapter that implements `AnalysisStrategy`

Current strategy packages:
- `strategy/ema_crossover`
- `strategy/breakout`
- `strategy/rsi_reversal`
- `strategy/volume_spike`

Important current state:
- strategy implementations exist
- volume spike strategy is currently enabled in `Main`

## EMA Crossover Strategy

Package:
`src/main/java/org/example/strategy/ema_crossover`

Main files:
- `EmaCrossoverAnalysisStrategy`
- `EmaCrossoverStrategy`
- `EmaCrossoverSignal`
- `EmaCrossoverStrategySettings`
- `EmaCalculator`

### Current Definition

The strategy no longer signals immediately on a fast/slow EMA crossover.

Current practical flow:
- detect fast EMA crossing slow EMA
- treat the crossover as a trend-state trigger
- wait for price to retest the fast EMA after the crossover
- signal when price comes close to the fast EMA

Current enabled settings in `Main`:
- fast EMA: `50`
- slow EMA: `100`
- retest tolerance: `10%`
- retest window: unlimited until the next crossover

Default settings for the strategy no-argument constructor:
- fast EMA: `50`
- slow EMA: `100`
- retest tolerance: `0.5%`
- retest window: unlimited until the next crossover

Retest tolerance can be configured through constructors:
- `new EmaCrossoverStrategySettings(50, 100, 0.25)`
- `new EmaCrossoverStrategySettings(50, 100, 0.25, 72)`

Bullish signal:
- EMA50 crosses above EMA100
- after the latest bullish crossover, candle range intersects the configured EMA50 proximity zone

Bearish signal:
- EMA50 crosses below EMA100
- after the latest bearish crossover, candle range intersects the configured EMA50 proximity zone

Retest tolerance formula:
- lower zone bound: `EMA50 * (1 - retestTolerancePercent / 100)`
- upper zone bound: `EMA50 * (1 + retestTolerancePercent / 100)`
- signal is valid when candle range `[low, high]` intersects that zone
- example: if EMA50 is `100` and `retestTolerancePercent` is `5`, the signal zone is `95..105`

Every new approach to EMA50 after the latest crossover is emitted as a signal.
If a new crossover happens, the previous crossover state is replaced and future retest signals belong only to the new crossover.
An approach means entering the EMA50 proximity zone from outside it.
While price remains inside or intersecting the zone, no duplicate signals are emitted.
After price leaves the zone, the next entry into the zone emits a new signal.

## Volume Spike Strategy

Package:
`src/main/java/org/example/strategy/volume_spike`

Main files:
- `VolumeSpikeAnalysisStrategy`
- `VolumeSpikeStrategy`
- `VolumeSpikeSignal`
- `VolumeSpikeStrategySettings`

### Current Definition

The strategy detects abnormal quote-volume expansion on hourly candles.

Current enabled settings in `Main`:
- lookback period: `48` candles
- spike multiplier: `3.0`
- signal cooldown: `6` candles

Detection rule:
- use `volumeto` as quote-volume
- calculate the median `volumeto` over the previous `lookbackPeriod` candles
- current candle is a volume spike if current `volumeto >= median * spikeMultiplier`
- median is used instead of average to avoid one previous outlier raising the baseline too much
- zero or missing-volume baselines are ignored
- neighboring spike signals are suppressed by cooldown

Signal direction:
- bullish candle creates a `BUY` signal
- bearish candle creates a `SELL` signal
- doji candles are ignored

## Detector Layer

The detector layer is for market structure / price action objects that are not necessarily trade signals by themselves.

Key contract:
`src/main/java/org/example/detector/MarketDetector.java`

```java
String getName();
DetectorResult<T> detect(Crypto asset, List<OhlcCandleDto> candles);
```

Normalized result:
`src/main/java/org/example/detector/DetectorResult.java`

Each detector package should ideally contain:
- detector logic
- detector-specific model objects
- optional shared logic if multiple detectors depend on the same price action definition

Current detector packages:
- `detector/imbalance`
- `detector/order_block`

## Current Enabled Component Model

In `Main`:

```java
List<AnalysisStrategy> enabledStrategies = List.of(
    ...
);

List<MarketDetector<?>> enabledDetectors = List.of(
    ...
);
```

This is still the active lightweight experiment-selection model.

Runtime toggles such as loop mode and Redis credentials are intentionally separate and live in `application.properties`.

## Assets

Assets are selected from:
`src/main/java/org/example/api/cryptocompare/Crypto.java`

The same enum also contains CoinDesk instrument identifiers:
- spot instrument, for example `BTC-USDT`
- futures instrument, for example `BTC-USDT-VANILLA-PERPETUAL`

CoinDesk exchange market parameters are represented by:
`src/main/java/org/example/api/coindesk/CoinDeskExchange.java`

CoinDesk latest tick responses are currently parsed with:
- `PRICE`
- `BEST_BID`
- `BEST_ASK`

For delta-neutral spot/perpetual monitoring, the executable long-spot/short-perp spread is calculated as:
`futuresBestBid - spotBestAsk`

If one side of bid/ask data is missing from CoinDesk latest tick, the current test logging falls back to:
`futuresLastPrice - spotLastPrice`
and marks the log line with `spreadMode=LAST_PRICE_FALLBACK`.

Example:

```java
List<Crypto> assets = List.of(
        Crypto.BTC,
        Crypto.ETH
);
```

To add a new asset:
- add enum value to `Crypto`
- include it in `assets` inside `Main`

## Imbalance Detector

Package:
`src/main/java/org/example/detector/imbalance`

Main files:
- `ImbalanceDetector`
- `Imbalance`
- `ImbalanceType`
- `ImbalanceLogic`

### Current Practical Definition

The project no longer uses only a naive strict wick-gap definition.

Current imbalance detection is hybrid and more practical for crypto:

For bullish imbalance:
- middle candle must be bullish
- middle candle body must be at least 50% of its total range
- third candle must close above the first candle high
- first attempt is classic wick gap
- if wick gap does not exist, fallback is body imbalance
- imbalance zone must not be too small

For bearish imbalance:
- middle candle must be bearish
- middle candle body must be at least 50% of its total range
- third candle must close below the first candle low
- first attempt is classic wick gap
- if wick gap does not exist, fallback is body imbalance
- imbalance zone must not be too small

### Untested Imbalance Rule

Important business rule:
- detector must return only untested imbalances

Meaning of "tested":
- price must actually enter inside the imbalance zone later
- boundary touch alone is not enough

Implementation rule:
- strict overlap is required
- simple touch of border does not invalidate the zone

### Overlapping Window Rule

An imbalance is based on a 3-candle structure.

To avoid duplicated neighboring detections from overlapping sliding windows:
- adjacent same-side imbalances from overlapping windows are suppressed

Example of what should not happen:
- one bearish imbalance from candles `A-B-C`
- another separate bearish imbalance immediately from `B-C-D`

For this project, that is treated as duplicate structure noise, not as two independent imbalances.

### Imbalance Output

`Imbalance.toString()` includes:
- raw unix timestamp
- readable UTC time in parentheses
- bounds
- type

Example format:

```text
Imbalance[createdAtTime=1778914800 (2026-05-16T07:00:00Z), lowerBound=2214.91, upperBound=2221.69, type=BEARISH]
```

## Order Block Detector

Package:
`src/main/java/org/example/detector/order_block`

Main files:
- `OrderBlockDetector`
- `OrderBlock`
- `OrderBlockType`

### Current Definition

Current order block logic is intentionally simple and tied to the same displacement logic as imbalances.

Bullish order block:
- middle candle in 3-candle structure must be bearish
- bullish imbalance logic around that structure must be valid
- zone is the full range of the middle candle

Bearish order block:
- middle candle in 3-candle structure must be bullish
- bearish imbalance logic around that structure must be valid
- zone is the full range of the middle candle

### Untested Order Block Rule

Only untested order blocks are returned.

Meaning:
- if later price enters inside the order block range, it is considered tested
- simple boundary touch is not enough

`OrderBlock.toString()` also includes:
- raw unix timestamp
- readable UTC time
- bounds
- type

## Redis Key Model

Current Redis publishing is intentionally split into:

### 1. Operational Current State

Per asset:
- `analysis:current:<ASSET>`

Contains:
- asset symbol
- candles loaded
- status
- detector counts under fields like:
  - `detector.imbalance_detector.count`
  - `detector.order_block_detector.count`

### 2. Active Detection Index

Per asset and detector:
- `analysis:active:<ASSET>:<DETECTOR>`

Contains:
- ids of currently active untested detections

### 3. Detection Detail Records

Per detection:
- `analysis:detection:<ASSET>:<DETECTOR>:<ID>`

Contains details such as:
- asset
- detector
- detection class
- detection type
- bounds
- created time
- status
- payload string

### 4. History Streams

Per asset and component:
- `analysis:history:<ASSET>:detector:<DETECTOR>`
- `analysis:history:<ASSET>:strategy:<STRATEGY>`

These are append-only short history streams with max length trimming.

### 5. Grafana Table Snapshot

Single current-cycle stream:
- `analysis:grafana:table`

Behavior:
- cleared at the start of every cycle
- one stream row is added per processed asset

Each row currently contains only:
- `asset`
- `imbalance_count`
- `order_block_count`
- `time`

This key exists specifically to simplify Grafana table panels.

## Redis / Grafana Local Stack

The repo now includes local infrastructure for dashboard work:
- `docker-compose.yml`
- Redis
- Redis Insight
- Grafana
- Redis ACL file under `redis/users.acl`

Current Redis local auth:
- username is configured
- password is configured
- Java app reads them from `application.properties`

## Console Output

Console output is still enabled through `ConsoleAnalysisResultPublisher`.

It prints:
- cycle started / finished markers
- asset section headers
- strategy signal counts and lines
- detector counts and detection objects
- asset-level failures

This means the project now has both:
- console output for local inspection
- Redis output for dashboards

## Current Project Conventions

When continuing work on this project, assume:

- `Main` should stay thin in terms of orchestration, but assets/detector activation still belongs there
- strategies should stay self-contained in their own packages
- detectors should stay self-contained in their own packages
- shared candle loading per asset is required
- avoid putting domain logic directly into `Main`
- prefer simple extensible contracts over heavy abstraction
- runtime settings that are not experiment selection can live in `application.properties`
- Grafana-specific Redis keys should stay minimal and purpose-built

## Known Limitations

- output is still console + Redis only
- no database persistence layer exists
- no retry/backoff policy yet
- no dedicated unit test suite yet for detectors or publishers
- detector settings are still mostly hardcoded in logic classes
- order block logic is still a simplified heuristic, not a full smart-money model
- local environment may not have `mvn` or `javac` available in `PATH`
- Grafana integration is currently current-snapshot oriented, not long-term analytics oriented
- Redis history is limited by stream trimming, not archival storage

## Recommended Next Steps

Good next improvements:

1. Add unit tests for:
   - imbalance detection
   - untested invalidation
   - overlapping window suppression
   - order block detection
   - Redis publisher snapshot behavior
2. Replace direct console output with structured logging behind the publisher layer.
3. Decide whether strategy/detector activation should eventually become configurable outside `Main`.
4. Add explicit detector settings objects, especially for imbalance thresholds.
5. Decide whether Redis history keys are still needed for the intended Grafana use cases.
6. Add a clean Grafana provisioning setup if dashboards/datasources should be reproducible.
7. Decide whether order block zones should use full candle range or body-only range.
8. Decide whether imbalance detector should support explicit modes:
   - wick only
   - body only
   - hybrid

## Last Known Intent

The user wants the project to evolve toward a more production-ready structure while keeping experimentation easy.

The current focus is:
- searching for entry setups
- reusing the same market data across multiple analyses
- running the analyzer in a loop every `N` seconds
- publishing current detector state into Redis
- exposing a simple Grafana table that shows, per asset, current imbalance and order block counts
- keeping activation of assets, strategies, and detectors simple from `Main`
