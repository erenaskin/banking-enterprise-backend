package com.banking.identity.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
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
            System.out.println("Embedded Redis started on port 6370");
        } catch (Exception e) {
            // Redis zaten çalışıyorsa veya başlatılamıyorsa sessizce devam et
            System.out.println("Embedded Redis could not be started: " + e.getMessage());
            System.out.println("Proceeding without embedded Redis - test will attempt connection to localhost:6370");
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            try {
                redisServer.stop();
                System.out.println("Embedded Redis stopped");
            } catch (Exception e) {
                System.out.println("Error stopping embedded Redis: " + e.getMessage());
            }
        }
    }

    /**
     * Provide a test RedisConnectionFactory that connects to embedded Redis on port 6370
     */
    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setHostName("localhost");
        factory.setPort(6370);
        // Lazy initialize to prevent connection errors on startup
        factory.setValidateConnection(false);
        return factory;
    }

    /**
     * Provide a test RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}