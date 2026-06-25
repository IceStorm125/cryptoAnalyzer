package org.example.api.cryptocompare;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.example.api.cryptocompare.dto.CryptoCompareHistoryResponseDto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;

public class CryptoCompareHttpClient {

    private static final String DEFAULT_BASE_URL = "https://min-api.cryptocompare.com/data/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final String DEFAULT_TO_SYMBOL = "USD";
    public static final String API_KEY = "0a655f51022435bb0cad113fa80d7b0170c34ac6825c1a647a520d1be27875d5";

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CryptoCompareHttpClient() {
        this(DEFAULT_BASE_URL, defaultObjectMapper(), defaultHttpClient());
    }

    public CryptoCompareHttpClient(String baseUrl) {
        this(baseUrl, defaultObjectMapper(), defaultHttpClient());
    }

    public CryptoCompareHttpClient(String baseUrl, ObjectMapper objectMapper) {
        this(baseUrl, objectMapper, defaultHttpClient());
    }

    public CryptoCompareHttpClient(String baseUrl, ObjectMapper objectMapper, HttpClient httpClient) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public CryptoCompareHistoryResponseDto getHourlyHistory(Crypto crypto, int limit)
            throws IOException, InterruptedException {
        String requestUrl = baseUrl
                + "v2/histohour?fsym=" + crypto.getSymbol()
                + "&tsym=" + DEFAULT_TO_SYMBOL
                + "&api_key=" + API_KEY
                + "&limit=" + limit;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(DEFAULT_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("CryptoCompare returned status " + statusCode + ": " + responseBody);
        }

        return objectMapper.readValue(responseBody, CryptoCompareHistoryResponseDto.class);
    }

    public List<OhlcCandleDto> getHourlyCandles(Crypto crypto, int limit)
            throws IOException, InterruptedException {
        CryptoCompareHistoryResponseDto response = getHourlyHistory(crypto, limit);
        if (response.getData() == null) {
            throw new IOException(buildInvalidResponseMessage(crypto, response, "Data section is missing"));
        }

        List<OhlcCandleDto> candles = response.getData().getData();
        if (candles == null) {
            throw new IOException(buildInvalidResponseMessage(crypto, response, "Candles list is missing"));
        }

        return Collections.unmodifiableList(candles);
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String buildInvalidResponseMessage(
            Crypto crypto,
            CryptoCompareHistoryResponseDto response,
            String reason
    ) {
        return "CryptoCompare returned invalid payload for "
                + crypto.getSymbol()
                + ". Reason: " + reason
                + ", response=" + response.getResponse()
                + ", type=" + response.getType()
                + ", message=" + response.getMessage();
    }
}
