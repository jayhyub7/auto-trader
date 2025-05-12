// 📁 com.auto.trader.global.exception.GlobalExceptionHandler.java

package com.auto.trader.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.auto.trader.config.entity.ExceptionLog;
import com.auto.trader.config.repository.ExceptionLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final ExceptionLogRepository exceptionLogRepository;

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex, HttpServletRequest request) {
    log.error("🚨 예외 발생: [{}] {} | URI: {}", ex.getClass().getSimpleName(), ex.getMessage(),
        request.getRequestURI(), ex);

    // ✅ DB 저장
    ExceptionLog logEntry = ExceptionLog.builder().exceptionType(ex.getClass().getSimpleName())
        .message(ex.getMessage()).uri(request.getRequestURI()).httpMethod(request.getMethod())
        .stackTrace(getStackTraceAsString(ex)).build();

    exceptionLogRepository.save(logEntry);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.");
  }

  private String getStackTraceAsString(Exception ex) {
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  // TODO: 필요에 따라 다른 예외들도 별도 정의 (e.g. ValidationException 등)
}
