package com.auto.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.auto.trader") // 명시적으로 엔티티 경로 지정
public class AutoTraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTraderApplication.class, args);
    }
}
