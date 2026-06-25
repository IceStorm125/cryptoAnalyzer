package org.example.config;

public final class LoggingSupport {

    private LoggingSupport() {
    }

    public static void configure(String appName, ApplicationSettings settings) {
        System.setProperty("APP_NAME", appName);
        System.setProperty("LOKI_URL", settings.loki().url());
        System.setProperty("LOKI_USERNAME", emptyIfNull(settings.loki().username()));
        System.setProperty("LOKI_PASSWORD", emptyIfNull(settings.loki().password()));
        System.setProperty("LOKI_TENANT_ID", emptyIfNull(settings.loki().tenantId()));
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
