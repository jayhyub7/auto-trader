// 파일 위치: src/main/java/com/auto/trader/util/CommonUtil.java
package com.auto.trader.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT); // 예쁘게 출력

    /**
     * 객체를 JSON 문자열로 변환
     *
     * @param object 변환할 객체 (List, DTO 등)
     * @return JSON 포맷 문자열 (에러 시 "JSON 변환 실패")
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("❌ JSON 변환 실패: {}", e.getMessage());
            return "JSON 변환 실패";
        }
    }
}
