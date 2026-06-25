package org.example.api.coindesk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.example.api.coindesk.dto.CoinDeskLatestTickDto;
import org.example.api.coindesk.dto.CoinDeskLatestTickResponseDto;
import org.example.api.cryptocompare.Crypto;

public class CoinDeskHttpClient {

    private static final String DEFAULT_BASE_URL = "https://data-api.coindesk.com/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CoinDeskHttpClient(String apiKey) {
        this(DEFAULT_BASE_URL, apiKey, defaultObjectMapper(), defaultHttpClient());
    }

    public CoinDeskHttpClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, defaultObjectMapper(), defaultHttpClient());
    }

    public CoinDeskHttpClient(String baseUrl, String apiKey,
                              ObjectMapper objectMapper,
                              HttpClient httpClient) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be blank");
        }

        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    // =========================
    // PUBLIC API
    // =========================

    public CoinDeskAssetPrice getSpotPrice(String market, String instrument)
            throws IOException, InterruptedException {
        return getLatestTick(CoinDeskMarketType.SPOT, market, instrument);
    }

    public CoinDeskAssetPrice getSpotPrice(CoinDeskExchange exchange, Crypto crypto)
            throws IOException, InterruptedException {
        validate(exchange, crypto);
        return getSpotPrice(exchange.getMarket(), crypto.getCoinDeskSpotInstrument());
    }

    public CoinDeskAssetPrice getFuturesPrice(String market, String instrument)
            throws IOException, InterruptedException {
        return getLatestTick(CoinDeskMarketType.FUTURES, market, instrument);
    }

    public CoinDeskAssetPrice getFuturesPrice(CoinDeskExchange exchange, Crypto crypto)
            throws IOException, InterruptedException {
        validate(exchange, crypto);
        return getFuturesPrice(exchange.getMarket(), crypto.getCoinDeskFuturesInstrument());
    }

    public CoinDeskAssetPrice getLatestTick(CoinDeskMarketType type,
                                            String market,
                                            String instrument)
            throws IOException, InterruptedException {

        validate(type, market, instrument);

        String url = buildUrl(type, market, instrument);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("CoinDesk API error: " + response.statusCode()
                    + " body=" + response.body());
        }

        CoinDeskLatestTickResponseDto dto =
                objectMapper.readValue(response.body(), CoinDeskLatestTickResponseDto.class);

        return extractPrice(type, market, instrument, dto);
    }

    // =========================
    // INTERNAL LOGIC
    // =========================

    private String buildUrl(CoinDeskMarketType type, String market, String instrument) {

        return baseUrl
                + type.getPath()
                + "?market=" + encode(market)
                + "&instruments=" + encode(instrument)
                + "&api_key=" + encode(apiKey);
    }

    private CoinDeskAssetPrice extractPrice(CoinDeskMarketType type,
                                            String market,
                                            String instrument,
                                            CoinDeskLatestTickResponseDto response) {

        Map<String, CoinDeskLatestTickDto> data = response.getData();

        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("Empty response data from CoinDesk");
        }

        CoinDeskLatestTickDto tick = data.get(instrument);

        if (tick == null) {
            throw new IllegalStateException("Instrument not found: " + instrument);
        }

        if (tick.getPrice() == null) {
            throw new IllegalStateException("Price missing for instrument: " + instrument);
        }

        return new CoinDeskAssetPrice(
                type,
                tick.getMarket() != null ? tick.getMarket() : market,
                tick.getInstrument() != null ? tick.getInstrument() : instrument,
                tick.getPrice(),
                tick.getPriceLastUpdateTimestamp(),
                tick.getBestBid(),
                tick.getBestBidLastUpdateTimestamp(),
                tick.getBestAsk(),
                tick.getBestAskLastUpdateTimestamp()
        );
    }

    // =========================
    // VALIDATION
    // =========================

    private static void validate(CoinDeskMarketType type, String market, String instrument) {
        if (type == null) {
            throw new IllegalArgumentException("Market type is null");
        }
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("Market is blank");
        }
        if (instrument == null || instrument.isBlank()) {
            throw new IllegalArgumentException("Instrument is blank");
        }
    }

    private static void validate(CoinDeskExchange exchange, Crypto crypto) {
        if (exchange == null) {
            throw new IllegalArgumentException("CoinDesk exchange is null");
        }
        if (crypto == null) {
            throw new IllegalArgumentException("Crypto asset is null");
        }
    }

    // =========================
    // UTIL
    // =========================

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
    }
}
