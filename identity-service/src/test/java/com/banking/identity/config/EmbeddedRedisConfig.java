package com.banking.identity.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
@Profile("test")
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        try {
            redisServer = new RedisServer(6370);
            redisServer.start();
        } catch (Exception e) {
            // Redis zaten çalışıyorsa veya başlatılamıyorsa sessizce devam et
            System.out.println("Embedded Redis could not be started: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}