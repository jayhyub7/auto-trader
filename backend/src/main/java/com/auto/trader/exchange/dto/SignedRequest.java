package com.auto.trader.exchange.dto;

import org.springframework.http.HttpHeaders;

public class SignedRequest {
    private final HttpHeaders headers;
    private final String queryString;

    public SignedRequest(HttpHeaders headers, String queryString) {
        this.headers = headers;
        this.queryString = queryString;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public String getQueryString() {
        return queryString;
    }
}
