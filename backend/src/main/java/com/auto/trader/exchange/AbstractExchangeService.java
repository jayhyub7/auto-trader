package com.auto.trader.exchange;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractExchangeService {

    protected final RestTemplate restTemplate = new RestTemplate();

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

    protected BalanceDto toBalanceDto(String asset, double available, double locked) {
        double total = available + locked;
        double usdRate = getUsdRate(asset);
        double usdValue = total * usdRate;
        double krwValue = usdValue * 1300; // 단순 고정 환율

        return new BalanceDto(asset, available, locked, total, usdValue, krwValue);
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
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(URI.create(url), HttpMethod.GET, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
    }
}
