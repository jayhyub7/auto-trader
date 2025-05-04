package com.auto.trader.exchange;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.auto.trader.balance.dto.BalanceDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchangeService {

    protected final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 바이낸스, 바이비트
     * @param data
     * @param secret
     * @return
     */
    protected String hmacSha256(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC 서명 실패", e);
        }
    }
    
    /**
     * 비트겟
     * @param data
     * @param secret
     * @return
     */
    protected String hmacSha256WithBase64Encoding(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC 서명 실패", e);
        }
    }

    protected BalanceDto toBalanceDto(String asset, double available, double locked) {
        double total = available + locked;
        double usdRate = getUsdRate(asset);
        double usdValue = total * usdRate;        

        return new BalanceDto(asset, available, locked, total, usdValue);
    }

    private double getUsdRate(String asset) {
        return switch (asset.toUpperCase()) {
            case "BTC" -> 60000.0;
            case "ETH" -> 3000.0;
            case "USDT", "USD" -> 1.0;
            default -> 0.0;
        };
    }

    protected ResponseEntity<Map<String, Object>> getWithHeaders(String url, HttpHeaders headers) {
        log.info("📤 요청 URL: {}", url);
        log.info("📤 요청 헤더:");
        headers.forEach((k, v) -> log.info("{}: {}", k, v));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> result = restTemplate.exchange(
            URI.create(url),
            HttpMethod.GET,
            requestEntity,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        log.info("📥 API 응답: {}", result.getBody());
        return result;
    }
}
