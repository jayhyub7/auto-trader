package com.auto.trader.config;


import org.springframework.context.annotation.Configuration;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
        System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
    }
}
