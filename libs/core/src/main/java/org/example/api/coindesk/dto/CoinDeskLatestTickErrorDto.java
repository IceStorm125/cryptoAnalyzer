package org.example.api.coindesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoinDeskLatestTickErrorDto {

    @JsonProperty("message")
    private String message;

    @JsonProperty("Message")
    private String legacyMessage;

    public String getMessage() {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return legacyMessage;
    }
}
